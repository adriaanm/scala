/* NSC -- new Scala compiler
 * Copyright 2005-2014 LAMP/EPFL
 * @author  Martin Odersky
 */

package scala.tools.nsc.backend.jvm

import scala.tools.nsc.Global

/**
 * This trait contains code shared between GenBCode and GenASM that depends on types defined in
 * the compiler cake (Global).
 */
final class BCodeAsmCommon[G <: Global](val global: G) {
  import global._
  import definitions._

  val ExcludedForwarderFlags = {
    import scala.tools.nsc.symtab.Flags._
    // Should include DEFERRED but this breaks findMember.
    SPECIALIZED | LIFTED | PROTECTED | STATIC | EXPANDEDNAME | BridgeAndPrivateFlags | MACRO
  }

  /**
   * Cache the value of delambdafy == "inline" for each run. We need to query this value many
   * times, so caching makes sense.
   */
  object delambdafyInline {
    private var runId = -1
    private var value = false

    def apply(): Boolean = {
      if (runId != global.currentRunId) {
        runId = global.currentRunId
        value = settings.Ydelambdafy.value == "inline"
      }
      value
    }
  }

  /**
   * True if `classSym` is an anonymous class or a local class. I.e., false if `classSym` is a
   * member class. This method is used to decide if we should emit an EnclosingMethod attribute.
   * It is also used to decide whether the "owner" field in the InnerClass attribute should be
   * null.
   */
  def isAnonymousOrLocalClass(classSym: Symbol): Boolean = {
    assert(classSym.isClass, s"not a class: $classSym")
    val r = exitingPickler(classSym.isAnonymousClass) || !classSym.originalOwner.isClass
    if (r && settings.Ybackend.value == "GenBCode") {
      // this assertion only holds in GenBCode. lambda lift renames symbols and may accidentally
      // introduce `$lambda` into a class name, making `isDelambdafyFunction` true. under GenBCode
      // we prevent this, see `nonAnon` in LambdaLift.
      // phase travel necessary: after flatten, the name includes the name of outer classes.
      // if some outer name contains $lambda, a non-lambda class is considered lambda.
      assert(exitingPickler(!classSym.isDelambdafyFunction), classSym.name)
    }
    r
  }

  /**
   * The next enclosing definition in the source structure. Includes anonymous function classes
   * under delambdafy:inline, even though they are only generated during UnCurry.
   */
  def nextEnclosing(sym: Symbol): Symbol = {
    val origOwner = sym.originalOwner
    // phase travel necessary: after flatten, the name includes the name of outer classes.
    // if some outer name contains $anon, a non-anon class is considered anon.
    if (delambdafyInline() && sym.rawowner.isAnonymousFunction) {
      // SI-9105: special handling for anonymous functions under delambdafy:inline.
      //
      //   class C { def t = () => { def f { class Z } } }
      //
      //   class C { def t = byNameMethod { def f { class Z } } }
      //
      // In both examples, the method f lambda-lifted into the anonfun class.
      //
      // In both examples, the enclosing method of Z is f, the enclosing class is the anonfun.
      // So nextEnclosing needs to return the following chain:  Z - f - anonFunClassSym - ...
      //
      // In the first example, the initial owner of f is a TermSymbol named "$anonfun" (note: not the anonFunClassSym!)
      // In the second, the initial owner of f is t (no anon fun term symbol for by-name args!).
      //
      // In both cases, the rawowner of class Z is the anonFunClassSym. So the check in the `if`
      // above makes sure we don't jump over the anonymous function in the by-name argument case.
      //
      // However, we cannot directly return the rawowner: if `sym` is Z, we need to include method f
      // in the result. This is done by comparing the rawowners (read: lambdalift-targets) of `sym`
      // and `sym.originalOwner`: if they are the same, then the originalOwner is "in between", and
      // we need to return it.
      // If the rawowners are different, the symbol was not in between. In the first example, the
      // originalOwner of `f` is the anonfun-term-symbol, whose rawowner is C. So the nextEnclosing
      // of `f` is its rawowner, the anonFunClassSym.
      //
      // In delambdafy:method we don't have that problem. The f method is lambda-lifted into C,
      // not into the anonymous function class. The originalOwner chain is Z - f - C.
      if (sym.originalOwner.rawowner == sym.rawowner) sym.originalOwner
      else sym.rawowner
    } else {
      origOwner
    }
  }

  /**
   * Returns the enclosing method for non-member classes. In the following example
   *
   * class A {
   *   def f = {
   *     class B {
   *       class C
   *     }
   *   }
   * }
   *
   * the method returns Some(f) for B, but None for C, because C is a member class. For non-member
   * classes that are not enclosed by a method, it returns None:
   *
   * class A {
   *   { class B }
   * }
   *
   * In this case, for B, we return None.
   *
   * The EnclosingMethod attribute needs to be added to non-member classes (see doc in BTypes).
   * This is a source-level property, so we need to use the originalOwner chain to reconstruct it.
   */
  private def enclosingMethodForEnclosingMethodAttribute(classSym: Symbol): Option[Symbol] = {
    assert(classSym.isClass, classSym)
    def enclosingMethod(sym: Symbol): Option[Symbol] = {
      if (sym.isClass || sym == NoSymbol) None
      else if (sym.isMethod) Some(sym)
      else enclosingMethod(nextEnclosing(sym))
    }
    enclosingMethod(nextEnclosing(classSym))
  }

  /**
   * The enclosing class for emitting the EnclosingMethod attribute. Since this is a source-level
   * property, this method looks at the originalOwner chain. See doc in BTypes.
   */
  private def enclosingClassForEnclosingMethodAttribute(classSym: Symbol): Symbol = {
    assert(classSym.isClass, classSym)
    def enclosingClass(sym: Symbol): Symbol = {
      if (sym.isClass) sym
      else enclosingClass(nextEnclosing(sym))
    }
    enclosingClass(nextEnclosing(classSym))
  }

  final case class EnclosingMethodEntry(owner: String, name: String, methodDescriptor: String)

  /**
   * Data for emitting an EnclosingMethod attribute. None if `classSym` is a member class (not
   * an anonymous or local class). See doc in BTypes.
   *
   * The class is parametrized by two functions to obtain a bytecode class descriptor for a class
   * symbol, and to obtain a method signature descriptor fro a method symbol. These function depend
   * on the implementation of GenASM / GenBCode, so they need to be passed in.
   */
  def enclosingMethodAttribute(classSym: Symbol, classDesc: Symbol => String, methodDesc: Symbol => String): Option[EnclosingMethodEntry] = {
    if (isAnonymousOrLocalClass(classSym)) {
      val methodOpt = enclosingMethodForEnclosingMethodAttribute(classSym)
      debuglog(s"enclosing method for $classSym is $methodOpt (in ${methodOpt.map(_.enclClass)})")
      Some(EnclosingMethodEntry(
        classDesc(enclosingClassForEnclosingMethodAttribute(classSym)),
        methodOpt.map(_.javaSimpleName.toString).orNull,
        methodOpt.map(methodDesc).orNull))
    } else {
      None
    }
  }

  /**
   * This is basically a re-implementation of sym.isStaticOwner, but using the originalOwner chain.
   *
   * The problem is that we are interested in a source-level property. Various phases changed the
   * symbol's properties in the meantime, mostly lambdalift modified (destructively) the owner.
   * Therefore, `sym.isStatic` is not what we want. For example, in
   *   object T { def f { object U } }
   * the owner of U is T, so UModuleClass.isStatic is true. Phase travel does not help here.
   */
  def isOriginallyStaticOwner(sym: Symbol): Boolean = {
    sym.isPackageClass || sym.isModuleClass && isOriginallyStaticOwner(sym.originalOwner)
  }

  /**
   * The member classes of a class symbol. Note that the result of this method depends on the
   * current phase, for example, after lambdalift, all local classes become member of the enclosing
   * class.
   */
  def memberClassesOf(classSymbol: Symbol): List[Symbol] = classSymbol.info.decls.collect({
    case sym if sym.isClass =>
      sym
    case sym if sym.isModule =>
      val r = exitingPickler(sym.moduleClass)
      assert(r != NoSymbol, sym.fullLocationString)
      r
  })(collection.breakOut)

  lazy val AnnotationRetentionPolicyModule       = AnnotationRetentionPolicyAttr.companionModule
  lazy val AnnotationRetentionPolicySourceValue  = AnnotationRetentionPolicyModule.tpe.member(TermName("SOURCE"))
  lazy val AnnotationRetentionPolicyClassValue   = AnnotationRetentionPolicyModule.tpe.member(TermName("CLASS"))
  lazy val AnnotationRetentionPolicyRuntimeValue = AnnotationRetentionPolicyModule.tpe.member(TermName("RUNTIME"))

  /** Whether an annotation should be emitted as a Java annotation
    * .initialize: if 'annot' is read from pickle, atp might be un-initialized
    */
  def shouldEmitAnnotation(annot: AnnotationInfo) = {
    annot.symbol.initialize.isJavaDefined &&
      annot.matches(ClassfileAnnotationClass) &&
      retentionPolicyOf(annot) != AnnotationRetentionPolicySourceValue &&
      annot.args.isEmpty
  }

  def isRuntimeVisible(annot: AnnotationInfo): Boolean = {
    annot.atp.typeSymbol.getAnnotation(AnnotationRetentionAttr) match {
      case Some(retentionAnnot) =>
        retentionAnnot.assocs.contains(nme.value -> LiteralAnnotArg(Constant(AnnotationRetentionPolicyRuntimeValue)))
      case _ =>
        // SI-8926: if the annotation class symbol doesn't have a @RetentionPolicy annotation, the
        // annotation is emitted with visibility `RUNTIME`
        true
    }
  }

  private def retentionPolicyOf(annot: AnnotationInfo): Symbol =
    annot.atp.typeSymbol.getAnnotation(AnnotationRetentionAttr).map(_.assocs).map(assoc =>
      assoc.collectFirst {
        case (`nme`.value, LiteralAnnotArg(Constant(value: Symbol))) => value
      }).flatten.getOrElse(AnnotationRetentionPolicyClassValue)

  def implementedInterfaces(classSym: Symbol): List[Symbol] = {
    // Additional interface parents based on annotations and other cues
    def newParentForAnnotation(ann: AnnotationInfo): Option[Type] = ann.symbol match {
      case RemoteAttr => Some(RemoteInterfaceClass.tpe)
      case _          => None
    }

    def isInterfaceOrTrait(sym: Symbol) = sym.isInterface || sym.isTrait

    val allParents = classSym.info.parents ++ classSym.annotations.flatMap(newParentForAnnotation)

    // We keep the superClass when computing minimizeParents to eliminate more interfaces.
    // Example: T can be eliminated from D
    //   trait T
    //   class C extends T
    //   class D extends C with T
    val interfaces = erasure.minimizeParents(allParents) match {
      case superClass :: ifs if !isInterfaceOrTrait(superClass.typeSymbol) =>
        ifs
      case ifs =>
        // minimizeParents removes the superclass if it's redundant, for example:
        //  trait A
        //  class C extends Object with A  // minimizeParents removes Object
        ifs
    }
    interfaces.map(_.typeSymbol)
  }
}
