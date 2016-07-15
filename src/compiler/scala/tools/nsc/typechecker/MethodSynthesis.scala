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

    // No field for these vals (either not emitted, not emitted yet, or removed later...):
    //   - abstract vals have no value we could store (until they become concrete, potentially)
    //   - lazy vals
    //   - concrete vals in traits don't yield a field here either (their getter's RHS has the initial value)
    //     Constructors will move the assignment to the constructor, abstracting over the field using the field setter,
    //     and Fields will add a field to the class that mixes in the trait, implementing the accessors in terms of it
    //   - [Emitted, later removed during Constructors] a concrete val with a statically known value (ConstantType)
    //     performs its side effect according to lazy/strict semantics, but doesn't need to store its value
    //     each access will "evaluate" the RHS (a literal) again
    // We would like to avoid emitting unnecessary fields, but the required knowledge isn't available until after typer.
    // The only way to avoid emitting & suppressing, is to not emit at all until we are sure to need the field, as dotty does.
    def noFieldFor(vd: ValDef) = vd.mods.isDeferred || vd.mods.isLazy || (owner.isTrait && !vd.mods.hasFlag(PRESUPER))

    // populate synthetics for this unit with trees that will later be added by the typer
    // we get here when entering the symbol for the valdef, so its rhs has not yet been type checked
    def enterGetterSetter(tree: ValDef): Unit = {
      val fieldSym =
        if (noFieldFor(tree)) NoSymbol
        else owner.newValue(tree.name.localName, tree.pos, tree.mods.flags & FieldFlags | PrivateLocal)

      val getter = Getter(tree)
      val getterSym = getter.createSym

      // only one symbol can have `tree.pos`, the others must focus their position
      // normally the field gets the range position, but if there is none, give it to the getter
      tree.symbol = fieldSym orElse (getterSym setPos tree.pos)
      val namer = namerOf(tree.symbol)

      def enter(deriver: DerivedAccessor, sym: Symbol) = {
        context.unit.synthetics(sym) = deriver.derivedTree(sym)
        sym setInfo namer.accessorTypeCompleter(tree, tree.tpt.isEmpty, isBean = false, deriver.isInstanceOf[Setter])
        enterInScope(sym)
      }

      // the valdef gets the accessor symbol for a lazy val (too much going on in its RHS)
      // the fields phase creates the field symbol
      if (!tree.mods.isLazy) {
        enter(getter, getterSym)

        if (getter.needsSetter) {
          val setter = Setter(tree)
          enter(setter, setter.createSym)
        }

        if (fieldSym != NoSymbol) {
          fieldSym setInfo namer.valTypeCompleter(tree)
          enterInScope(fieldSym)
        }
      } else {
        getterSym setInfo namer.valTypeCompleter(tree)
        enterInScope(getterSym)
      }

      deriveBeanAccessors(tree, namer)
    }

    private def deriveBeanAccessors(tree: ValDef, namer: Namer): Unit = {
      // TODO: can we look at the annotations symbols? (name-based introduced in 8cc477f8b6, see neg/t3403)
      val hasBeanProperty = tree.mods hasAnnotationNamed tpnme.BeanPropertyAnnot
      val hasBoolBP = tree.mods hasAnnotationNamed tpnme.BooleanBeanPropertyAnnot

      if (hasBeanProperty || hasBoolBP) {
        if (!tree.name.charAt(0).isLetter) BeanPropertyAnnotationFieldWithoutLetterError(tree)
        // avoids name clashes with private fields in traits
        else if (tree.mods.isPrivate) BeanPropertyAnnotationPrivateFieldError(tree)

        val derivedPos = tree.pos.focus
        val missingTpt = tree.tpt.isEmpty

        def deriveBeanAccessor(prefix: String): Symbol = {
          val isSetter = prefix == "set"
          val name = newTermName(prefix + tree.name.toString.capitalize)
          val setterParam = nme.syntheticParamName(1)

          // note: tree.tpt may be EmptyTree, which will be a problem when use as the tpt of a parameter
          // the completer will patch this up (we can't do this now without completing the field)
          val tptToPatch = if (missingTpt) TypeTree() else tree.tpt.duplicate

          val (vparams, tpt) =
            if (isSetter) (List(ValDef(Modifiers(PARAM | SYNTHETIC), setterParam, tptToPatch, EmptyTree)), TypeTree(UnitTpe))
            else (Nil, tptToPatch)

          val rhs =
            if (tree.mods.isDeferred) EmptyTree
            else if (isSetter) Apply(Ident(tree.name.setterName), List(Ident(setterParam)))
            else Select(This(owner), tree.name)

          val sym = createMethod(tree, name, derivedPos, tree.mods.flags & BeanPropertyFlags)
          context.unit.synthetics(sym) = newDefDef(sym, rhs)(tparams = Nil, vparamss = List(vparams), tpt = tpt)
          sym
        }

        val getterCompleter = namer.accessorTypeCompleter(tree, missingTpt, isBean = true, isSetter = false)
        enterInScope(deriveBeanAccessor(if (hasBeanProperty) "get" else "is") setInfo getterCompleter)

        if (tree.mods.isMutable) {
          val setterCompleter = namer.accessorTypeCompleter(tree, missingTpt, isBean = true, isSetter = true)
          enterInScope(deriveBeanAccessor("set") setInfo setterCompleter)
        }
      }
    }



    def addDerivedTrees(typer: Typer, stat: Tree): List[Tree] = stat match {
      case cd@ClassDef(mods, _, _, _) if mods.isImplicit =>
        import AnnotationInfo.{mkFilter => annotationFilter}
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


    /** A synthetic method which performs the implicit conversion implied by
      *  the declaration of an implicit class.
      */
    case class ImplicitClassWrapper(tree: ClassDef) {
      def derivedSym = {
        val enclClass = tree.symbol.owner.enclClass
        // Only methods will do! Don't want to pick up any stray
        // companion objects of the same name.
        val result = enclClass.info decl derivedName filter (x => x.isMethod && x.isSynthetic)
        if (result == NoSymbol || result.isOverloaded)
          context.error(tree.pos, s"Internal error: Unable to find the synthetic factory method corresponding to implicit class $derivedName in $enclClass / ${enclClass.info.decls}")
        result
      }

      def derivedTree = factoryMeth(derivedMods, derivedName, tree)

      def derivedName = tree.name.toTermName
      def derivedMods = tree.mods & AccessFlags | METHOD | IMPLICIT | SYNTHETIC
    }


    trait DerivedAccessor {
      def tree: ValDef
      def derivedName: TermName
      def derivedFlags: Long
      def derivedTree(sym: Symbol): Tree

      def derivedPos = tree.pos.focus
      def createSym = createMethod(tree, derivedName, derivedPos, derivedFlags)
    }

    case class Getter(tree: ValDef) extends DerivedAccessor {
      def derivedName  = tree.name
      def derivedFlags = tree.mods.flags & GetterFlags | ACCESSOR.toLong | ( if (needsSetter) 0 else STABLE )
      def needsSetter  = tree.mods.isMutable  // implies !lazy

      override def derivedTree(derivedSym: Symbol) = {
        val missingTpt = tree.tpt.isEmpty
        val tpt = if (missingTpt) TypeTree() else tree.tpt.duplicate

        val rhs =
          if (noFieldFor(tree)) tree.rhs // context.unit.transformed.getOrElse(tree.rhs, tree.rhs)
          else Select(This(tree.symbol.enclClass), tree.symbol)

        newDefDef(derivedSym, rhs)(tparams = Nil, vparamss = ListOfNil, tpt = tpt) // don't look at derivedSym's info!
      }

//      private def deriveLazyAccessor(derivedSym: Symbol): DefDef = {
//        val missingTpt = tree.tpt.isEmpty
//        val tpt = if (missingTpt) TypeTree() else tree.tpt.duplicate
//
//        derivedSym setPos tree.pos
//        // ValDef will have its position focused whereas DefDef will have original correct rangepos
//        // ideally positions would be correct at the creation time but lazy vals are really a special case
//        // here so for the sake of keeping api clean we fix positions manually in LazyValGetter
//        tpt.setPos(tree.tpt.pos)
//        tree.tpt.setPos(tree.tpt.pos.focus)
//
//        newDefDef(derivedSym, rhs)(tparams = Nil, vparamss = ListOfNil, tpt = tpt) // don't look at derivedSym's info!
//      }

//      // TODO: more principled approach -- this is a bit bizarre
//      private def derivedTpt(derivedSym: Symbol) = {
//        // For existentials, don't specify a type for the getter, even one derived
//        // from the symbol! This leads to incompatible existentials for the field and
//        // the getter. Let the typer do all the work. You might think "why only for
//        // existentials, why not always," and you would be right, except: a single test
//        // fails, but it looked like some work to deal with it. Test neg/t0606.scala
//        // starts compiling (instead of failing like it's supposed to) because the typer
//        // expects to be able to identify escaping locals in typedDefDef, and fails to
//        // spot that brand of them. In other words it's an artifact of the implementation.
//        val getterTp = derivedSym.tpe_*.finalResultType
//        val tpt = getterTp.widen match {
//          // Range position errors ensue if we don't duplicate this in some
//          // circumstances (at least: concrete vals with existential types.)
//          case _: ExistentialType => TypeTree() setOriginal (tree.tpt.duplicate setPos tree.tpt.pos.focus)
//          case _ if tree.mods.isDeferred    => TypeTree() setOriginal tree.tpt // keep type tree of original abstract field
//          case _                  => TypeTree(getterTp)
//        }
//        tpt setPos tree.tpt.pos.focus
//      }

    }

    case class Setter(tree: ValDef) extends DerivedAccessor {
      def derivedName  = tree.setterName
      def derivedFlags = tree.mods.flags & SetterFlags | ACCESSOR
      def derivedTree(derivedSym: Symbol)  = {
        val setterParam = nme.syntheticParamName(1)

        // note: tree.tpt may be EmptyTree, which will be a problem when use as the tpt of a parameter
        // the completer will patch this up (we can't do this now without completing the field)
        val missingTpt = tree.tpt.isEmpty
        val tptToPatch = if (missingTpt) TypeTree() else tree.tpt.duplicate

        val vparams = List(ValDef(Modifiers(PARAM | SYNTHETIC), setterParam, tptToPatch, EmptyTree))

        val tpt = TypeTree(UnitTpe)

        val rhs =
          if (noFieldFor(tree)) EmptyTree
          else Assign(Select(This(tree.symbol.enclClass), tree.symbol), Ident(setterParam))

        newDefDef(derivedSym, rhs)(tparams = Nil, vparamss = List(vparams), tpt = tpt)

      }
    }

  }
}
