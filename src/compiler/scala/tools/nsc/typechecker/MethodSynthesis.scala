/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author  Paul Phillips
 */
package scala.tools.nsc
package typechecker

import symtab.Flags._
import scala.reflect.internal.util.StringOps.ojoin
import scala.reflect.internal.util.ListOfNil

/** Logic related to method synthesis which involves cooperation between
 *  Namer and Typer.
 */
trait MethodSynthesis {
  self: Analyzer =>

  import global._
  import definitions._
  import CODE._


  class ClassMethodSynthesis(val clazz: Symbol, localTyper: Typer) {
    def mkThis = This(clazz) setPos clazz.pos.focus
    def mkThisSelect(sym: Symbol) = atPos(clazz.pos.focus)(
      if (clazz.isClass) Select(This(clazz), sym) else Ident(sym)
    )

    private def isOverride(name: TermName) =
      clazzMember(name).alternatives exists (sym => !sym.isDeferred && (sym.owner != clazz))

    def newMethodFlags(name: TermName) = {
      val overrideFlag = if (isOverride(name)) OVERRIDE else 0L
      overrideFlag | SYNTHETIC
    }
    def newMethodFlags(method: Symbol) = {
      val overrideFlag = if (isOverride(method.name.toTermName)) OVERRIDE else 0L
      (method.flags | overrideFlag | SYNTHETIC) & ~DEFERRED
    }

    private def finishMethod(method: Symbol, f: Symbol => Tree): Tree =
      localTyper typed (
        if (method.isLazy) ValDef(method, f(method))
        else DefDef(method, f(method))
      )

    private def createInternal(name: Name, f: Symbol => Tree, info: Type): Tree = {
      val name1 = name.toTermName
      val m = clazz.newMethod(name1, clazz.pos.focus, newMethodFlags(name1))
      finishMethod(m setInfoAndEnter info, f)
    }
    private def createInternal(name: Name, f: Symbol => Tree, infoFn: Symbol => Type): Tree = {
      val name1 = name.toTermName
      val m = clazz.newMethod(name1, clazz.pos.focus, newMethodFlags(name1))
      finishMethod(m setInfoAndEnter infoFn(m), f)
    }
    private def cloneInternal(original: Symbol, f: Symbol => Tree, name: Name): Tree = {
      val m = original.cloneSymbol(clazz, newMethodFlags(original), name) setPos clazz.pos.focus
      finishMethod(clazz.info.decls enter m, f)
    }

    def clazzMember(name: Name)  = clazz.info nonPrivateMember name
    def typeInClazz(sym: Symbol) = clazz.thisType memberType sym

    def deriveMethod(original: Symbol, nameFn: Name => Name)(f: Symbol => Tree): Tree =
      cloneInternal(original, f, nameFn(original.name))

    def createMethod(name: Name, paramTypes: List[Type], returnType: Type)(f: Symbol => Tree): Tree =
      createInternal(name, f, (m: Symbol) => MethodType(m newSyntheticValueParams paramTypes, returnType))

    def createMethod(name: Name, returnType: Type)(f: Symbol => Tree): Tree =
      createInternal(name, f, NullaryMethodType(returnType))

    def createMethod(original: Symbol)(f: Symbol => Tree): Tree =
      createInternal(original.name, f, original.info)

    def forwardMethod(original: Symbol, newMethod: Symbol)(transformArgs: List[Tree] => List[Tree]): Tree =
      createMethod(original)(m => gen.mkMethodCall(newMethod, transformArgs(m.paramss.head map Ident)))

    def createSwitchMethod(name: Name, range: Seq[Int], returnType: Type)(f: Int => Tree) = {
      createMethod(name, List(IntTpe), returnType) { m =>
        val arg0    = Ident(m.firstParam)
        val default = DEFAULT ==> Throw(IndexOutOfBoundsExceptionClass.tpe_*, fn(arg0, nme.toString_))
        val cases   = range.map(num => CASE(LIT(num)) ==> f(num)).toList :+ default

        Match(arg0, cases)
      }
    }

    // def foo() = constant
    def constantMethod(name: Name, value: Any): Tree = {
      val constant = Constant(value)
      createMethod(name, Nil, constant.tpe)(_ => Literal(constant))
    }
    // def foo = constant
    def constantNullary(name: Name, value: Any): Tree = {
      val constant = Constant(value)
      createMethod(name, constant.tpe)(_ => Literal(constant))
    }
  }

  /** There are two key methods in here.
   *
   *   1) Enter methods such as enterGetterSetter are called
   *   from Namer with a tree which may generate further trees such as accessors or
   *   implicit wrappers. Some setup is performed.  In general this creates symbols
   *   and enters them into the scope of the owner.
   *
   *   2) addDerivedTrees is called from Typer when a Template is typed.
   *   It completes the job, returning a list of trees with their symbols
   *   set to those created in the enter methods.  Those trees then become
   *   part of the typed template.
   */
  trait MethodSynth {
    self: Namer =>

    import NamerErrorGen._

    def enterImplicitWrapper(tree: ClassDef): Unit = {
      enterSyntheticSym(ImplicitClassWrapper(tree).derivedTree)
    }

    // trees are later created by addDerivedTrees (common logic is encapsulated in field/standardAccessors/beanAccessors)
    def enterGetterSetter(tree: ValDef): Unit = {
      val fields = field(tree)

      val accessors@(getter :: _) = standardAccessors(tree)
      val accessorSyms@(getterSym :: setterSyms) = accessors.map(_.newAccessorSymbol)

      // a lazy field is linked to its lazy accessor (TODO: can we do the same for field -> getter -> setter)
      val fieldSym = if (fields.nonEmpty) fields.head.newFieldSymbol(getterSym) else NoSymbol
      val namer = if (fields.nonEmpty) namerOf(fieldSym) else namerOf(getterSym)

      // TODO: why change the getter's position -- it's already at `tree.pos.focus`
      tree.symbol = fieldSym orElse (getterSym setPos tree.pos)

      if (fieldSym != NoSymbol) fieldSym setInfo namer.valTypeCompleter(tree)


      // There's no reliable way to detect all kinds of setters from flags or name!!!
      // A BeanSetter's name does not end in `_=` -- it does begin with "set", but so could the getter
      // for a regular Scala field... TODO: can we add a flag to distinguish getter/setter accessors?
      val getterCompleter = namer.accessorTypeCompleter(tree, isSetter = false)
      val setterCompleter = namer.accessorTypeCompleter(tree, isSetter = true)

      getterSym setInfo getterCompleter
      setterSyms foreach (_ setInfo setterCompleter)

      accessorSyms foreach enterInScope
      if (fieldSym != NoSymbol) enterInScope(fieldSym)

      val beans = beanAccessorsFromNames(tree)
      if (beans.nonEmpty) {
        def deriveTree(bean: BeanAccessor, sym: Symbol): DefDef = {
          val mods = bean.derivedMods mapAnnotations (_ => Nil)

          val setterParam = nme.syntheticParamName(1)

          val tptToPatch = tree.tpt.duplicate

          // note: tree.tpt may be EmptyTree, which will be a problem when use as the tpt of a parameter
          // the completer will patch this up (we can't do this now without completing the field)
          val (vparams, tpt) =
            if (bean.isInstanceOf[BeanSetter]) (List(ValDef(Modifiers(PARAM | SYNTHETIC), setterParam, tptToPatch, EmptyTree)), TypeTree(UnitTpe))
            else (Nil, tptToPatch)

          val rhs =
            if (bean.isDeferred) EmptyTree
            else if (bean.isInstanceOf[BeanSetter]) Apply(Ident(tree.name.setterName), List(Ident(setterParam)))
            else Select(This(owner), tree.name)

          val ddef: DefDef = atPos(tree.pos.focus)(DefDef(mods, bean.name, Nil, List(vparams), tpt, rhs))
          ddef.symbol = sym
          context.unit.synthetics(sym) = ddef

          ddef
        }

        if (!tree.name.charAt(0).isLetter)
          BeanPropertyAnnotationFieldWithoutLetterError(tree)
        else if (tree.mods.isPrivate)  // avoids name clashes with private fields in traits
          BeanPropertyAnnotationPrivateFieldError(tree)

        val accessorSyms@(getterSym :: setterSyms) = beans.map { b =>
          val sym = b.newAccessorSymbol
          deriveTree(b, sym)
          sym
        }

        val getterCompleter = namer.beanAccessorTypeCompleter(tree, isSetter = false)
        val setterCompleter = namer.beanAccessorTypeCompleter(tree, isSetter = true)

        getterSym setInfo getterCompleter
        setterSyms foreach (_ setInfo setterCompleter)

        accessorSyms foreach enterInScope
      }

    }


    import AnnotationInfo.{mkFilter => annotationFilter}

//    /** This is called for those ValDefs which addDerivedTrees ignores, but
//     *  which might have a warnable annotation situation.
//     */
//    private def warnForDroppedValAnnotations(sym: Symbol) {
//      val targetClass   = if (sym.isValueParameter || sym.isParamAccessor) ParamTargetClass else FieldTargetClass
//      val annotations   = sym.initialize.annotations
//      val retained      = annotations filter annotationFilter(targetClass, defaultRetention = true)
//
//      annotations filterNot (retained contains _) foreach (ann => issueAnnotationWarning(sym, ann, targetClass))
//    }
//    private def issueAnnotationWarning(sym: Symbol, ann: AnnotationInfo, defaultTarget: Symbol) {
//      global.reporter.warning(ann.pos,
//        s"no valid targets for annotation on $sym - it is discarded unused. " +
//        s"You may specify targets with meta-annotations, e.g. @($ann @${defaultTarget.name})")
//    }

    def addDerivedTrees(typer: Typer, stat: Tree): List[Tree] = stat match {
      case vd @ ValDef(mods, name, tpt, rhs) if deriveAccessors(vd) && !vd.symbol.isModuleVar =>
        stat.symbol.initialize // needed!

        ((field(vd) ::: standardAccessors(vd))
              map { acc => acc.validate() ; atPos(vd.pos.focus)(acc.derivedTree) }
        filterNot (_ eq EmptyTree))
      case cd @ ClassDef(mods, _, _, _) if mods.isImplicit =>
        val annotations = stat.symbol.initialize.annotations
        // TODO: need to shuffle annotations between wrapper and class.
        val wrapper = ImplicitClassWrapper(cd)
        val meth = wrapper.derivedSym
        context.unit.synthetics get meth match {
          case Some(mdef) =>
            context.unit.synthetics -= meth
            meth setAnnotations (annotations filter annotationFilter(MethodTargetClass, defaultRetention = false))
            cd.symbol setAnnotations (annotations filter annotationFilter(ClassTargetClass, defaultRetention = true))
            List(cd, mdef)
          case _ =>
            // Shouldn't happen, but let's give ourselves a reasonable error when it does
            context.error(cd.pos, s"Internal error: Symbol for synthetic factory method not found among ${context.unit.synthetics.keys.mkString(", ")}")
            // Soldier on for the sake of the presentation compiler
            List(cd)
        }
      case _ =>
        stat :: Nil
      }


    def field(vd: ValDef): List[Field] = if (Field.noFieldFor(vd)) Nil else List(Field(vd))

    // getter is head of resulting list
    def standardAccessors(vd: ValDef): List[DerivedFromValDef] =
      if (vd.mods.isLazy) List(LazyValGetter(vd))
      else {
        val getter = Getter(vd)
        if (getter.needsSetter) List(getter, Setter(vd))
        else List(getter)
      }

    def beanAccessors(vd: ValDef): List[DerivedFromValDef] = {
      val setter = if (vd.mods.isMutable) List(BeanSetter(vd)) else Nil
      if (vd.symbol hasAnnotation BeanPropertyAttr)
        BeanGetter(vd) :: setter
      else if (vd.symbol hasAnnotation BooleanBeanPropertyAttr)
        BooleanBeanGetter(vd) :: setter
      else Nil
    }

    // same as beanAccessors, but without needing symbols -- TODO: can we use the symbol-based variant? (name-based introduced in 8cc477f8b6)
    private def beanAccessorsFromNames(tree: ValDef): List[BeanAccessor] = {
      val hasBP     = tree.mods hasAnnotationNamed tpnme.BeanPropertyAnnot
      val hasBoolBP = tree.mods hasAnnotationNamed tpnme.BooleanBeanPropertyAnnot

      if (!hasBP && !hasBoolBP) Nil
      else (if (hasBP) new BeanGetter(tree) else new BooleanBeanGetter(tree)) ::
        (if (tree.mods.isMutable) List(BeanSetter(tree)) else Nil)
    }


    /** This trait assembles what's needed for synthesizing derived methods.
     *  Important: Typically, instances of this trait are created TWICE for each derived
     *  symbol; once form Namers in an enter method, and once from Typers in addDerivedTrees.
     *  So it's important that creating an instance of Derived does not have a side effect,
     *  or if it has a side effect, control that it is done only once.
     */
    sealed trait Derived {

      /** The tree from which we are deriving a synthetic member. Typically, that's
       *  given as an argument of the instance. */
      def tree: Tree

      /** The name of the method */
      def name: TermName

      /** The flags that are retained from the original symbol */
      def flagsMask: Long

      /** The flags that the derived symbol has in addition to those retained from
       *  the original symbol*/
      def flagsExtra: Long

      /** The derived symbol. It is assumed that this symbol already exists and has been
       *  entered in the parent scope when derivedSym is called */
      def derivedSym: Symbol

      /** The definition tree of the derived symbol. */
      def derivedTree: Tree
    }

    sealed trait DerivedFromMemberDef extends Derived {
      def tree: MemberDef
      def enclClass: Symbol

      // Final methods to make the rest easier to reason about.
      final def mods        = tree.mods
      final def basisSym    = tree.symbol
      final def derivedMods = mods & flagsMask | flagsExtra
    }

    sealed trait DerivedFromClassDef extends DerivedFromMemberDef {
      def tree: ClassDef
      final def enclClass = basisSym.owner.enclClass
    }

    sealed trait DerivedFromValDef extends DerivedFromMemberDef {
      def tree: ValDef
      final def enclClass = basisSym.enclClass

      final def fieldSelection         = Select(This(enclClass), basisSym)

      def derivedSym: Symbol = tree.symbol
      def derivedTree: Tree  = EmptyTree

      final def newAccessorSymbol: MethodSymbol = {
        val sym = owner.newMethod(name, tree.pos.focus, derivedMods.flags)
        setPrivateWithin(tree, sym)
        sym
      }

      def isDeferred = mods.isDeferred
      def validate() { }

      private def logDerived(result: Tree): Tree = {
        debuglog("[+derived] " + ojoin(mods.flagString, basisSym.accurateKindString, basisSym.getterName.decode)
          + " (" + derivedSym + ")\n        " + result)

        result
      }
    }

    sealed trait DerivedGetter extends DerivedFromValDef {
      def needsSetter = mods.isMutable
    }
    sealed trait DerivedSetter extends DerivedFromValDef {
      protected def setterParam = derivedSym.paramss match {
        case (p :: Nil) :: _  => p
        case _                => NoSymbol
      }

      protected def setterRhs = {
        assert(!derivedSym.isOverloaded, s"Unexpected overloaded setter $derivedSym for $basisSym in $enclClass")
        if (Field.noFieldFor(tree) || derivedSym.isOverloaded) EmptyTree
        else Assign(fieldSelection, Ident(setterParam))
      }

      private def setterDef = DefDef(derivedSym, setterRhs)
      override def derivedTree: Tree = if (setterParam == NoSymbol) EmptyTree else setterDef
    }

    /** A synthetic method which performs the implicit conversion implied by
     *  the declaration of an implicit class.
     */
    case class ImplicitClassWrapper(tree: ClassDef) extends DerivedFromClassDef {
      def derivedSym: Symbol = {
        // Only methods will do! Don't want to pick up any stray
        // companion objects of the same name.
        val result = enclClass.info decl name filter (x => x.isMethod && x.isSynthetic)
        if (result == NoSymbol || result.isOverloaded)
          context.error(tree.pos, s"Internal error: Unable to find the synthetic factory method corresponding to implicit class $name in $enclClass / ${enclClass.info.decls}")
        result
      }
      def derivedTree: DefDef          = factoryMeth(derivedMods, name, tree)
      def flagsExtra: Long             = METHOD | IMPLICIT | SYNTHETIC
      def flagsMask: Long              = AccessFlags
      def name: TermName               = tree.name.toTermName
    }

    sealed abstract class BaseGetter(tree: ValDef) extends DerivedGetter {
      def name       = tree.name
      def flagsMask  = GetterFlags
      def flagsExtra = ACCESSOR.toLong | ( if (tree.mods.isMutable) 0 else STABLE )

      override def validate() {
        assert(derivedSym != NoSymbol, tree)
        if (derivedSym.isOverloaded)
          GetterDefinedTwiceError(derivedSym)

        super.validate()
      }
    }

    case class Getter(tree: ValDef) extends BaseGetter(tree) {
      override def derivedSym = if (Field.noFieldFor(tree)) basisSym else basisSym.getterIn(enclClass)
      private def derivedRhs  = if (Field.noFieldFor(tree)) tree.rhs else fieldSelection

      // TODO: more principled approach -- this is a bit bizarre
      private def derivedTpt = {
        // For existentials, don't specify a type for the getter, even one derived
        // from the symbol! This leads to incompatible existentials for the field and
        // the getter. Let the typer do all the work. You might think "why only for
        // existentials, why not always," and you would be right, except: a single test
        // fails, but it looked like some work to deal with it. Test neg/t0606.scala
        // starts compiling (instead of failing like it's supposed to) because the typer
        // expects to be able to identify escaping locals in typedDefDef, and fails to
        // spot that brand of them. In other words it's an artifact of the implementation.
        val getterTp = derivedSym.tpe_*.finalResultType
        val tpt = getterTp.widen match {
          // Range position errors ensue if we don't duplicate this in some
          // circumstances (at least: concrete vals with existential types.)
          case _: ExistentialType => TypeTree() setOriginal (tree.tpt.duplicate setPos tree.tpt.pos.focus)
          case _ if isDeferred    => TypeTree() setOriginal tree.tpt // keep type tree of original abstract field
          case _                  => TypeTree(getterTp)
        }
        tpt setPos tree.tpt.pos.focus
      }
      override def derivedTree: DefDef = newDefDef(derivedSym, derivedRhs)(tpt = derivedTpt)
    }

    /** Implements lazy value accessors:
      *    - for lazy values of type Unit and all lazy fields inside traits,
      *      the rhs is the initializer itself, because we'll just "compute" the result on every access
      *     ("computing" unit / constant type is free -- the side-effect is still only run once, using the init bitmap)
      *    - for all other lazy values z the accessor is a block of this form:
      *      { z = <rhs>; z } where z can be an identifier or a field.
      */
    case class LazyValGetter(tree: ValDef) extends BaseGetter(tree) {
      class ChangeOwnerAndModuleClassTraverser(oldowner: Symbol, newowner: Symbol)
        extends ChangeOwnerTraverser(oldowner, newowner) {

        override def traverse(tree: Tree) {
          tree match {
            case _: DefTree => change(tree.symbol.moduleClass)
            case _          =>
          }
          super.traverse(tree)
        }
      }

      // todo: in future this should be enabled but now other phases still depend on the flag for various reasons
      //override def flagsMask = (super.flagsMask & ~LAZY)
      override def derivedSym = basisSym.lazyAccessor
      override def derivedTree: DefDef = {
        val ValDef(_, _, tpt0, rhs0) = tree
        val rhs1 = context.unit.transformed.getOrElse(rhs0, rhs0)
        val body =
          if (tree.symbol.owner.isTrait || Field.noFieldFor(tree)) rhs1 // TODO move tree.symbol.owner.isTrait into noFieldFor
          else gen.mkAssignAndReturn(basisSym, rhs1)

        derivedSym setPos tree.pos // cannot set it at createAndEnterSymbol because basisSym can possibly still have NoPosition
        val ddefRes = DefDef(derivedSym, new ChangeOwnerAndModuleClassTraverser(basisSym, derivedSym)(body))
        // ValDef will have its position focused whereas DefDef will have original correct rangepos
        // ideally positions would be correct at the creation time but lazy vals are really a special case
        // here so for the sake of keeping api clean we fix positions manually in LazyValGetter
        ddefRes.tpt.setPos(tpt0.pos)
        tpt0.setPos(tpt0.pos.focus)
        ddefRes
      }
    }
    case class Setter(tree: ValDef) extends DerivedSetter {
      def name       = tree.setterName
      def flagsMask  = SetterFlags
      def flagsExtra = ACCESSOR

      // TODO: double check logic behind need for name expansion in context of new fields phase
      override def derivedSym = basisSym.setterIn(enclClass)
    }

    object Field {
      // No field for these vals (either never emitted or eliminated later on):
      //   - abstract vals have no value we could store (until they become concrete, potentially)
      //   - lazy vals of type Unit
      //   - concrete vals in traits don't yield a field here either (their getter's RHS has the initial value)
      //     Constructors will move the assignment to the constructor, abstracting over the field using the field setter,
      //     and Fields will add a field to the class that mixes in the trait, implementing the accessors in terms of it
      //   - [Emitted, later removed during Constructors] a concrete val with a statically known value (ConstantType)
      //     performs its side effect according to lazy/strict semantics, but doesn't need to store its value
      //     each access will "evaluate" the RHS (a literal) again
      // We would like to avoid emitting unnecessary fields, but the required knowledge isn't available until after typer.
      // The only way to avoid emitting & suppressing, is to not emit at all until we are sure to need the field, as dotty does.
      // NOTE: do not look at `vd.symbol` when called from `enterGetterSetter` (luckily, that call-site implies `!mods.isLazy`),
      // similarly, the `def field` call-site breaks when you add `|| vd.symbol.owner.isTrait` (detected in test suite)
      // as the symbol info is in the process of being created then.
      // TODO: harmonize tree & symbol creation
      // the middle  `&& !owner.isTrait` is needed after `isLazy` because non-unit-typed lazy vals in traits still get a field -- see neg/t5455.scala
      def noFieldFor(vd: ValDef) = (vd.mods.isDeferred
        || (vd.mods.isLazy && !owner.isTrait && isUnitType(vd.symbol.info))
        || (owner.isTrait && !traitFieldFor(vd)))

      // TODO: never emit any fields in traits -- only use getter for lazy/presuper ones as well
      private def traitFieldFor(vd: ValDef): Boolean = vd.mods.hasFlag(PRESUPER | LAZY)
    }

    case class Field(tree: ValDef) extends DerivedFromValDef {
      def name       = tree.localName
      def flagsMask  = FieldFlags
      def flagsExtra = PrivateLocal

      private val isLazy = mods.isLazy

      override def derivedTree =
        if (isLazy) copyValDef(tree)(mods = mods | flagsExtra, name = this.name, rhs = EmptyTree).setPos(tree.pos.focus)
        else copyValDef(tree)(mods = mods | flagsExtra, name = this.name)

      def newFieldSymbol(getter: MethodSymbol) = {
        // If the owner is not a class, this is a lazy val from a method,
        // with no associated field.  It has an accessor with $lzy appended to its name and
        // its flags are set differently.  The implicit flag is reset because otherwise
        // a local implicit "lazy val x" will create an ambiguity with itself
        // via "x$lzy" as can be seen in test #3927.
        val localLazyVal = isLazy && !owner.isClass
        val name = if (localLazyVal) tree.name append nme.LAZY_LOCAL else tree.localName
        val flags =
          if (localLazyVal) (mods.flags | ARTIFACT | MUTABLE) & ~IMPLICIT
          else mods.flags & FieldFlags | PrivateLocal | (if (isLazy) MUTABLE else 0)

        val sym = owner.newValue(name, tree.pos, flags)

        if (isLazy) sym setLazyAccessor getter

        sym
      }

    }

    sealed abstract class BeanAccessor(bean: String) extends DerivedFromValDef {
      val name       = newTermName(bean + tree.name.toString.capitalize)
      def flagsMask  = BeanPropertyFlags
      def flagsExtra = 0
      override def derivedSym = enclClass.info decl name
    }
    sealed trait AnyBeanGetter extends BeanAccessor with DerivedGetter {
      override def validate() {
        if (derivedSym == NoSymbol) {
          // the namer decides whether to generate these symbols or not. at that point, we don't
          // have symbolic information yet, so we only look for annotations named "BeanProperty".
          BeanPropertyAnnotationLimitationError(tree)
        }
        super.validate()
      }
    }

    // NoSymbolBeanGetter synthesizes the getter's RHS (which defers to the regular setter)
    // (not sure why, but there is one use site of the BeanGetters where NoSymbolBeanGetter is not mixed in)
    // TODO: clean this up...
    case class BooleanBeanGetter(tree: ValDef) extends BeanAccessor("is") with AnyBeanGetter
    case class BeanGetter(tree: ValDef) extends BeanAccessor("get") with AnyBeanGetter

    // the bean setter's RHS delegates to the setter
    case class BeanSetter(tree: ValDef) extends BeanAccessor("set") with DerivedSetter {
      override protected def setterRhs = Apply(Ident(tree.name.setterName), List(Ident(setterParam)))
    }
  }
}
