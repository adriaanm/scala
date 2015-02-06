/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author
 */

package scala.tools.nsc
package transform

import symtab._
import Flags._
import scala.collection.{ mutable, immutable }
import scala.collection.mutable.{ LinkedHashMap, LinkedHashSet, TreeSet }

abstract class LambdaLift extends InfoTransform {
  import global._
  import definitions._

  /** the following two members override abstract members in Transform */
  val phaseName: String = "lambdalift"

  private val lifted = new TypeMap {
    def apply(tp: Type): Type = tp match {
      case TypeRef(NoPrefix, sym, Nil) if sym.isClass && !sym.isPackageClass =>
        typeRef(apply(sym.owner.enclClass.thisType), sym, Nil)
      case ClassInfoType(parents, decls, clazz) =>
        val parents1 = parents mapConserve this
        if (parents1 eq parents) tp
        else ClassInfoType(parents1, decls, clazz)
      case _ =>
        mapOver(tp)
    }
  }

  /** scala.runtime.*Ref classes */
  private lazy val allRefClasses: Set[Symbol] = {
    refClass.values.toSet ++ volatileRefClass.values.toSet ++ Set(VolatileObjectRefClass, ObjectRefClass)
  }

  /** Each scala.runtime.*Ref class has a static method `create(value)` that simply instantiates the Ref to carry that value. */
  private lazy val refCreateMethod: Map[Symbol, Symbol] = {
    mapFrom(allRefClasses.toList)(x => getMemberMethod(x.companionModule, nme.create))
  }

  /** Quite frequently a *Ref is initialized with its zero (e.g., null, 0.toByte, etc.) Method `zero()` of *Ref class encapsulates that pattern. */
  private lazy val refZeroMethod: Map[Symbol, Symbol] = {
    mapFrom(allRefClasses.toList)(x => getMemberMethod(x.companionModule, nme.zero))
  }

  def transformInfo(sym: Symbol, tp: Type): Type =
    if (sym.isCapturedVariable) capturedVariableType(sym, tpe = lifted(tp), erasedTypes = true)
    else lifted(tp)

  protected def newTransformer(unit: CompilationUnit): Transformer =
    new LambdaLifter(unit)

  class LambdaLifter(unit: CompilationUnit) extends explicitOuter.OuterPathTransformer(unit) {

    private type SymSet = TreeSet[Symbol]

    /** A map storing free variables of functions and classes */
    private val free = new LinkedHashMap[Symbol, SymSet]

    /** A map storing the free variable proxies of functions and classes */
    private val proxies = new LinkedHashMap[Symbol, List[Symbol]]

    /** A hashtable storing calls between functions */
    private val called = new LinkedHashMap[Symbol, SymSet]

    /** Symbols that are called from an inner class. */
    private val calledFromInner = new LinkedHashSet[Symbol]

    private val ord = Ordering.fromLessThan[Symbol](_ isLess _)
    private def newSymSet = TreeSet.empty[Symbol](ord)

    private def symSet(f: LinkedHashMap[Symbol, SymSet], sym: Symbol): SymSet =
      f.getOrElseUpdate(sym, newSymSet)

    /** The set of symbols that need to be renamed. */
    private val renamable = newSymSet

    /**
     * The new names for free variables proxies. If we simply renamed the
     * free variables, we would transform:
     * {{{
     *   def closure(x: Int) = { () => x }
     * }}}
     *
     * To:
     * {{{
     *   def closure(x$1: Int) = new anonFun$1(this, x$1)
     *   class anonFun$1(outer$: Outer, x$1: Int) { def apply() => x$1 }
     * }}}
     *
     * This is fatally bad for named arguments (0e170e4b), extremely impolite to tools
     * reflecting on the method parameter names in the generated bytecode (SI-6028),
     * and needlessly bothersome to anyone using a debugger.
     *
     * Instead, we transform to:
     * {{{
     *   def closure(x: Int) = new anonFun$1(this, x)
     *   class anonFun$1(outer$: Outer, x$1: Int) { def apply() => x$1 }
     * }}}
     */
    private val proxyNames       = mutable.HashMap[Symbol, Name]()

    // (trait, name) -> owner
    private val localTraits      = mutable.HashMap[(Symbol, Name), Symbol]()
    // (owner, name) -> implClass
    private val localImplClasses = mutable.HashMap[(Symbol, Name), Symbol]()

    /** A flag to indicate whether new free variables have been found */
    private var changedFreeVars: Boolean = _

    /** Buffers for lifted out classes and methods */
    private val liftedDefs = new LinkedHashMap[Symbol, List[Tree]]

    private def isSameOwnerEnclosure(sym: Symbol) =
      sym.owner.logicallyEnclosingMember == currentOwner.logicallyEnclosingMember

    /** Mark symbol `sym` as being free in `enclosure`, unless `sym`
     *  is defined in `enclosure` or there is a class between `enclosure`s owner
     *  and the owner of `sym`.
     *  Return `true` if there is no class between `enclosure` and
     *  the owner of sym.
     *  pre: sym.isLocalToBlock, (enclosure.isMethod || enclosure.isClass)
     *
     *  The idea of `markFree` is illustrated with an example:
     *
     *  def f(x: int) = {
     *    class C {
     *      class D {
     *        val y = x
     *      }
     *    }
     *  }
     *
     *  In this case `x` is free in the primary constructor of class `C`.
     *  but it is not free in `D`, because after lambda lift the code would be transformed
     *  as follows:
     *
     *  def f(x$0: int) {
     *    class C(x$0: int) {
     *      val x$1 = x$0
     *      class D {
     *        val y = outer.x$1
     *      }
     *    }
     *  }
     */
    private def markFree(sym: Symbol, enclosure: Symbol): Boolean = {
      debuglog("mark free: " + sym.fullLocationString + " marked free in " + enclosure)
      (enclosure == sym.owner.logicallyEnclosingMember) || {
        debuglog("%s != %s".format(enclosure, sym.owner.logicallyEnclosingMember))
        if (enclosure.isPackageClass || !markFree(sym, enclosure.skipConstructor.owner.logicallyEnclosingMember)) false
        else {
          val ss = symSet(free, enclosure)
          if (!ss(sym)) {
            ss += sym
            renamable += sym
            changedFreeVars = true
            debuglog("" + sym + " is free in " + enclosure)
            if (sym.isVariable) sym setFlag CAPTURED
          }
          !enclosure.isClass
        }
      }
    }

    private def markCalled(sym: Symbol, owner: Symbol) {
      debuglog("mark called: " + sym + " of " + sym.owner + " is called by " + owner)
      symSet(called, owner) += sym
      if (sym.enclClass != owner.enclClass) calledFromInner += sym
    }

    /** The traverse function */
    private val freeVarTraverser = new Traverser {
      override def traverse(tree: Tree) {
       try { //debug
        val sym = tree.symbol
        tree match {
          case ClassDef(_, _, _, _) =>
            liftedDefs(tree.symbol) = Nil
            if (sym.isLocalToBlock) {
              // Don't rename implementation classes independently of their interfaces. If
              // the interface is to be renamed, then we will rename the implementation
              // class at that time. You'd think we could call ".implClass" on the trait
              // rather than collecting them in another map, but that seems to fail for
              // exactly the traits being renamed here (i.e. defined in methods.)
              //
              // !!! - it makes no sense to have methods like "implClass" and
              // "companionClass" which fail for an arbitrary subset of nesting
              // arrangements, and then have separate methods which attempt to compensate
              // for that failure. There should be exactly one method for any given
              // entity which always gives the right answer.
              if (sym.isImplClass)
                localImplClasses((sym.owner, tpnme.interfaceName(sym.name))) = sym
              else {
                renamable += sym
                if (sym.isTrait)
                  localTraits((sym, sym.name)) = sym.owner
              }
            }
          case DefDef(_, _, _, _, _, _) =>
            if (sym.isLocalToBlock) {
              renamable += sym
              sym setFlag (PrivateLocal | FINAL)
            } else if (sym.isPrimaryConstructor) {
              symSet(called, sym) += sym.owner
            }
          case Ident(name) =>
            if (sym == NoSymbol) {
              assert(name == nme.WILDCARD)
            } else if (sym.isLocalToBlock) {
              val owner = currentOwner.logicallyEnclosingMember
              if (sym.isTerm && !sym.isMethod) markFree(sym, owner)
              else if (sym.isMethod) markCalled(sym, owner)
                //symSet(called, owner) += sym
            }
          case Select(_, _) =>
            if (sym.isConstructor && sym.owner.isLocalToBlock)
              markCalled(sym, currentOwner.logicallyEnclosingMember)
          case _ =>
        }
        super.traverse(tree)
       } catch {//debug
         case ex: Throwable =>
           Console.println(s"$ex while traversing $tree")
           throw ex
       }
      }
    }

    /** Compute free variables map `fvs`.
     *  Also assign unique names to all
     *  value/variable/let that are free in some function or class, and to
     *  all class/function symbols that are owned by some function.
     */
    private def computeFreeVars() {
      freeVarTraverser.traverse(unit.body)

      do {
        changedFreeVars = false
        for (caller <- called.keys ; callee <- called(caller) ; fvs <- free get callee ; fv <- fvs)
          markFree(fv, caller)
      } while (changedFreeVars)

      def renameSym(sym: Symbol) {
        val originalName = sym.name
        sym setName newName(sym)
        debuglog("renaming in %s: %s => %s".format(sym.owner.fullLocationString, originalName, sym.name))
      }

      // make sure that the name doesn't make the symbol accidentally `isAnonymousClass` (et.al) by
      // introducing `$anon` in its name. to be cautious, we don't make this change in the default
      // backend under 2.11.x, so only in GenBCode.
      def nonAnon(s: String) = if (settings.Ybackend.value == "GenBCode") nme.ensureNonAnon(s) else s

      def newName(sym: Symbol): Name = {
        val originalName = sym.name
        def freshen(prefix: String): Name =
          if (originalName.isTypeName) unit.freshTypeName(prefix)
          else unit.freshTermName(prefix)

        val join = nme.NAME_JOIN_STRING
        if (sym.isAnonymousFunction && sym.owner.isMethod) {
          freshen(sym.name + join + nonAnon(sym.owner.name.toString) + join)
        } else {
          val name = freshen(sym.name + join)
          // SI-5652 If the lifted symbol is accessed from an inner class, it will be made public. (where?)
          //         Generating a unique name, mangled with the enclosing full class name (including
          //         package - subclass might have the same name), avoids a VerifyError in the case
          //         that a sub-class happens to lifts out a method with the *same* name.
          if (originalName.isTermName && !sym.enclClass.isImplClass && calledFromInner(sym))
            newTermNameCached(nonAnon(sym.enclClass.fullName('$')) + nme.EXPAND_SEPARATOR_STRING + name)
          else
            name
        }
      }

      /* Rename a trait's interface and implementation class in coordinated fashion. */
      def renameTrait(traitSym: Symbol, implSym: Symbol) {
        val originalImplName = implSym.name
        renameSym(traitSym)
        implSym setName tpnme.implClassName(traitSym.name)

        debuglog("renaming impl class in step with %s: %s => %s".format(traitSym, originalImplName, implSym.name))
      }

      val allFree: Set[Symbol] = free.values.flatMap(_.iterator).toSet

      for (sym <- renamable) {
        // If we renamed a trait from Foo to Foo$1, we must rename the implementation
        // class from Foo$class to Foo$1$class.  (Without special consideration it would
        // become Foo$class$1 instead.) Since the symbols are being renamed out from
        // under us, and there's no reliable link between trait symbol and impl symbol,
        // we have maps from ((trait, name)) -> owner and ((owner, name)) -> impl.
        localTraits remove ((sym, sym.name)) match {
          case None        =>
            if (allFree(sym)) proxyNames(sym) = newName(sym)
            else renameSym(sym)
          case Some(owner) =>
            localImplClasses remove ((owner, sym.name)) match {
              case Some(implSym)  => renameTrait(sym, implSym)
              case _              => renameSym(sym) // pure interface, no impl class
            }
        }
      }

      afterOwnPhase {
        for ((owner, freeValues) <- free.toList) {
          val newFlags = SYNTHETIC | ( if (owner.isClass) PARAMACCESSOR | PrivateLocal else PARAM )
          debuglog("free var proxy: %s, %s".format(owner.fullLocationString, freeValues.toList.mkString(", ")))
          proxies(owner) =
            for (fv <- freeValues.toList) yield {
              val proxyName = proxyNames.getOrElse(fv, fv.name)
              val proxy = owner.newValue(proxyName.toTermName, owner.pos, newFlags.toLong) setInfo fv.info
              if (owner.isClass) owner.info.decls enter proxy
              proxy
            }
        }
      }
    }

    private def proxy(sym: Symbol) = {
      def searchIn(enclosure: Symbol): Symbol = {
        if (enclosure eq NoSymbol) throw new IllegalArgumentException("Could not find proxy for "+ sym.defString +" in "+ sym.ownerChain +" (currentOwner= "+ currentOwner +" )")
        debuglog("searching for " + sym + "(" + sym.owner + ") in " + enclosure + " " + enclosure.logicallyEnclosingMember)

        val proxyName = proxyNames.getOrElse(sym, sym.name)
        val ps = (proxies get enclosure.logicallyEnclosingMember).toList.flatten find (_.name == proxyName)
        ps getOrElse searchIn(enclosure.skipConstructor.owner)
      }
      debuglog("proxy %s from %s has logical enclosure %s".format(
        sym.debugLocationString,
        currentOwner.debugLocationString,
        sym.owner.logicallyEnclosingMember.debugLocationString)
      )

      if (isSameOwnerEnclosure(sym)) sym
      else searchIn(currentOwner)
    }

    private def memberRef(sym: Symbol): Tree = {
      val clazz = sym.owner.enclClass
      //Console.println("memberRef from "+currentClass+" to "+sym+" in "+clazz)
      def prematureSelfReference() {
        val what =
          if (clazz.isStaticOwner) clazz.fullLocationString
          else s"the unconstructed `this` of ${clazz.fullLocationString}"
        val msg = s"Implementation restriction: access of ${sym.fullLocationString} from ${currentClass.fullLocationString}, would require illegal premature access to $what"
        reporter.error(curTree.pos, msg)
      }
      val qual =
        if (clazz == currentClass) gen.mkAttributedThis(clazz)
        else {
          sym resetFlag (LOCAL | PRIVATE)
          if (isUnderConstruction(clazz)) {
            prematureSelfReference()
            EmptyTree
          }
          else if (clazz.isStaticOwner) gen.mkAttributedQualifier(clazz.thisType)
          else {
            outerValue match {
              case EmptyTree => prematureSelfReference(); return EmptyTree
              case o         => outerPath(o, currentClass.outerClass, clazz)
            }
          }
        }
      Select(qual, sym) setType sym.tpe
    }

    private def proxyRef(sym: Symbol) = {
      val psym = proxy(sym)
      if (psym.isLocalToBlock) gen.mkAttributedIdent(psym) else memberRef(psym)
    }

    private def addFreeArgs(pos: Position, sym: Symbol, args: List[Tree]) = {
      free get sym match {
        case Some(fvs) => args ++ (fvs.toList map (fv => atPos(pos)(proxyRef(fv))))
        case _         => args
      }
    }

    private def addFreeParams(tree: Tree, sym: Symbol): Tree = proxies.get(sym) match {
      case Some(ps) =>
        val freeParams = ps map (p => ValDef(p) setPos tree.pos setType NoType)
        tree match {
          case DefDef(_, _, _, vparams :: _, _, _) =>
            val addParams = cloneSymbols(ps).map(_.setFlag(PARAM))
            sym.updateInfo(
              lifted(MethodType(sym.info.params ::: addParams, sym.info.resultType)))

            copyDefDef(tree)(vparamss = List(vparams ++ freeParams))
          case ClassDef(_, _, _, _) =>
            // SI-6231
            // Disabled attempt to to add getters to freeParams
            // this does not work yet. Problem is that local symbols need local names
            // and references to local symbols need to be transformed into
            // method calls to setters.
            // def paramGetter(param: Symbol): Tree = {
            //   val getter = param.newGetter setFlag TRANS_FLAG resetFlag PARAMACCESSOR // mark because we have to add them to interface
            //   sym.info.decls.enter(getter)
            //   val rhs = Select(gen.mkAttributedThis(sym), param) setType param.tpe
            //   DefDef(getter, rhs) setPos tree.pos setType NoType
            // }
            // val newDefs = if (sym.isTrait) freeParams ::: (ps map paramGetter) else freeParams
            deriveClassDef(tree)(impl => deriveTemplate(impl)(_ ::: freeParams))
        }
      case None =>
        tree
    }

/*  SI-6231: Something like this will be necessary to eliminate the implementation
 *  restriction from paramGetter above:
 *  We need to pass getters to the interface of an implementation class.
    private def fixTraitGetters(lifted: List[Tree]): List[Tree] =
      for (stat <- lifted) yield stat match {
        case ClassDef(mods, name, tparams, templ @ Template(parents, self, body))
        if stat.symbol.isTrait && !stat.symbol.isImplClass =>
          val iface = stat.symbol
          lifted.find(l => l.symbol.isImplClass && l.symbol.toInterface == iface) match {
            case Some(implDef) =>
              val impl = implDef.symbol
              val implGetters = impl.info.decls.toList filter (_ hasFlag TRANS_FLAG)
              if (implGetters.nonEmpty) {
                val ifaceGetters = implGetters map { ig =>
                  ig resetFlag TRANS_FLAG
                  val getter = ig cloneSymbol iface setFlag DEFERRED
                  iface.info.decls enter getter
                  getter
                }
                val ifaceGetterDefs = ifaceGetters map (DefDef(_, EmptyTree) setType NoType)
                treeCopy.ClassDef(
                  stat, mods, name, tparams,
                  treeCopy.Template(templ, parents, self, body ::: ifaceGetterDefs))
              } else
                stat
            case None =>
              stat
          }
        case _ =>
          stat
      }
*/
    private def liftDef(tree: Tree): Tree = {
      val sym = tree.symbol
      val oldOwner = sym.owner
      if (sym.isMethod && isUnderConstruction(sym.owner.owner)) { // # bug 1909
         if (sym.isModule) { // Yes, it can be a module and a method, see comments on `isModuleNotMethod`!
           // TODO promote to an implementation restriction if we can reason that this *always* leads to VerifyError.
           // See neg/t1909-object.scala
           def msg = s"SI-1909 Unable to STATICally lift $sym, which is defined in the self- or super-constructor call of ${sym.owner.owner}. A VerifyError is likely."
           devWarning(tree.pos, msg)
          } else sym setFlag STATIC
      }

      sym.owner = sym.owner.enclClass
      if (sym.isClass) sym.owner = sym.owner.toInterface
      if (sym.isMethod) sym setFlag LIFTED
      liftedDefs(sym.owner) ::= tree
      // TODO: this modifies the ClassInfotype of the enclosing class, which is associated with another phase (explicitouter).
      // This breaks type history: in a phase travel to before lambda lift, the ClassInfoType will contain lifted classes.
      sym.owner.info.decls enterUnique sym
      debuglog("lifted: " + sym + " from " + oldOwner + " to " + sym.owner)
      EmptyTree
    }

    private def postTransform(tree: Tree, isBoxedRef: Boolean = false): Tree = {
      val sym = tree.symbol
      tree match {
        case ClassDef(_, _, _, _) =>
          val tree1 = addFreeParams(tree, sym)
          if (sym.isLocalToBlock) liftDef(tree1) else tree1
        case DefDef(_, _, _, _, _, _) =>
          val tree1 = addFreeParams(tree, sym)
          if (sym.isLocalToBlock) liftDef(tree1) else tree1
        case ValDef(mods, name, tpt, rhs) =>
          if (sym.isCapturedVariable) {
            val tpt1 = TypeTree(sym.tpe) setPos tpt.pos

            val refTypeSym = sym.tpe.typeSymbol

            val factoryCall = typer.typedPos(rhs.pos) {
              rhs match {
                case EmptyTree =>
                  val zeroMSym   = refZeroMethod(refTypeSym)
                  gen.mkMethodCall(zeroMSym, Nil)
                case arg =>
                  val createMSym = refCreateMethod(refTypeSym)
                  gen.mkMethodCall(createMSym, arg :: Nil)
              }
            }

            treeCopy.ValDef(tree, mods, name, tpt1, factoryCall)
          } else tree
        case Return(Block(stats, value)) =>
          Block(stats, treeCopy.Return(tree, value)) setType tree.tpe setPos tree.pos
        case Return(expr) =>
          assert(sym == currentMethod, sym)
          tree
        case Apply(fn, args) =>
          treeCopy.Apply(tree, fn, addFreeArgs(tree.pos, sym, args))
        case Assign(Apply(TypeApply(sel @ Select(qual, _), _), List()), rhs) =>
          // eliminate casts introduced by selecting a captured variable field
          // on the lhs of an assignment.
          assert(sel.symbol == Object_asInstanceOf)
          treeCopy.Assign(tree, qual, rhs)
        case Ident(name) =>
          val tree1 =
            if (sym.isTerm && !sym.isLabel)
              if (sym.isMethod)
                atPos(tree.pos)(memberRef(sym))
              else if (sym.isLocalToBlock && !isSameOwnerEnclosure(sym))
                atPos(tree.pos)(proxyRef(sym))
              else tree
            else tree
          if (sym.isCapturedVariable && !isBoxedRef)
            atPos(tree.pos) {
              val tp = tree.tpe
              val elemTree = typer typed Select(tree1 setType sym.tpe, nme.elem)
              if (elemTree.tpe.typeSymbol != tp.typeSymbol) gen.mkAttributedCast(elemTree, tp) else elemTree
            }
          else tree1
        case Block(stats, expr0) =>
          val (lzyVals, rest) = stats partition {
            case stat: ValDef => stat.symbol.isLazy || stat.symbol.isModuleVar
            case _            => false
          }
          if (lzyVals.isEmpty) tree
          else treeCopy.Block(tree, lzyVals ::: rest, expr0)
        case _ =>
          tree
      }
    }

    private def preTransform(tree: Tree) = super.transform(tree) setType lifted(tree.tpe)

    override def transform(tree: Tree): Tree = tree match {
      case Select(ReferenceToBoxed(idt), elem) if elem == nme.elem =>
        postTransform(preTransform(idt), isBoxedRef = false)
      case ReferenceToBoxed(idt) =>
        postTransform(preTransform(idt), isBoxedRef = true)
      case _ =>
        postTransform(preTransform(tree))
    }

    /** Transform statements and add lifted definitions to them. */
    override def transformStats(stats: List[Tree], exprOwner: Symbol): List[Tree] = {
      def addLifted(stat: Tree): Tree = stat match {
        case ClassDef(_, _, _, _) =>
          val lifted = liftedDefs get stat.symbol match {
            case Some(xs) => xs reverseMap addLifted
            case _        => log("unexpectedly no lifted defs for " + stat.symbol) ; Nil
          }
          try deriveClassDef(stat)(impl => deriveTemplate(impl)(_ ::: lifted))
          finally liftedDefs -= stat.symbol

        case DefDef(_, _, _, _, _, Block(Nil, expr)) if !stat.symbol.isConstructor =>
          deriveDefDef(stat)(_ => expr)
        case _ =>
          stat
      }
      super.transformStats(stats, exprOwner) map addLifted
    }

    override def transformUnit(unit: CompilationUnit) {
      computeFreeVars()
      afterOwnPhase {
        super.transformUnit(unit)
      }
      assert(liftedDefs.isEmpty, liftedDefs.keys mkString ", ")
    }
  } // class LambdaLifter

}
