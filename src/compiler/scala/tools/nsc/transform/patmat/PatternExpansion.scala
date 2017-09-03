/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author  Paul Phillips
 */

package scala
package tools
package nsc
package transform
package patmat

import scala.tools.nsc.typechecker.Contexts

/** An 'extractor' can be a case class or an unapply or unapplySeq method.
  *
  * In a case class, the class is the unextracted type and the fixed and
  * repeated types are derived from its constructor parameters.
  *
  * In an unapply, this is reversed: the parameter to the unapply is the
  * unextracted type, and the other types are derived based on the return
  * type of the unapply method.
  *
  * An extractor returns: F1, F2, ..., Fi, opt[Seq[E] or E*]
  * A case matches: P1, P2, ..., Pj, opt[Seq[E]]
  * Put together: P1/F1, P2/F2, ... Pi/Fi, Pi+1/E, Pi+2/E, ... Pj/E, opt[Seq[E]]
  *
  * Here Pm/Fi is the last pattern to match the fixed arity section.
  *
  * productArity: the value of i, i.e. the number of non-sequence types in the extractor
  * nonStarArity: the value of j, i.e. the number of non-star patterns in the case definition
  * elementArity: j - i, i.e. the number of non-star patterns which must match sequence elements
  * starArity: 1 or 0 based on whether there is a star (sequence-absorbing) pattern
  * totalArity: nonStarArity + starArity, i.e. the number of patterns in the case definition
  *
  * Note that productArity is a function only of the extractor, and
  * nonStar/star/totalArity are all functions of the patterns. The key
  * value for aligning and typing the patterns is elementArity, as it
  * is derived from both sets of information.
  *
  * If elementArity is...
  *   - zero: A perfect match between extractor and the fixed patterns.
  *        If there is a star pattern it will match any sequence.
  *   - positive: There are more patterns than products. There will have to be a
  *       sequence which can populate at least `elementArity` patterns.
  *   - negative: There are more products than patterns: compile time error.
  *
  */
trait PatternExpansion {
  val global: Global

  import global._
  import definitions._
  import treeInfo._

  def unapplyFormals(fun: Tree, args: List[Tree])(context: Contexts#Context): List[Type] =
    new ExtractorAlignment(fun, args)(context).unapplyFormals.map{case NoType => ErrorType case tp => tp}

  // Analyze the fun / args of a case class or extractor pattern in terms of repeated patterns etc.
  // Extracts some info from signatures of get/apply/head methods (name-based patmat)
  class ExtractorAlignment(fun: Tree, args: List[Tree])(context: Contexts#Context) {
    // based on args
    def isStar       = star != EmptyTree
    def starArity    = if (isStar) 1 else 0
    def nonStarArity = fixedPats.length
    def totalArity   = nonStarArity + starArity

    // based on fun
    def productArity = productTypes.length
    def elementArity = nonStarArity - productArity
    def isSeq = elementType ne NoType

    // the expected argument type of the (equivalent, for a case class) unapply method
    def expectedExtractedType =
      if (isUnapply || isUnapplySeq) firstParamType(fun.tpe)
      else fun.tpe.finalResultType // result type of the case class constructor

    def unapplyFormals: List[Type] =
      if (isSeq) productTypes :+ repeatedType else productTypes

    def subPatTypes: List[Type] = {
      val withoutStar = productTypes ::: List.fill(elementArity)(elementType)
      if (isStar) withoutStar :+ sequenceType else withoutStar
    }

    def isBool   = !isSeq && productArity == 0
    def isSingle = !isSeq && totalArity == 1

    def lengthCompareSym = sequenceType member nme.lengthCompare

    // rest is private
    private val (fixedPats, star) = args match {
      case init :+ last if treeInfo.isStar(last) => (init, last)
      case _ => (args, EmptyTree)
    }

    private val isUnapply        = fun.symbol.name == nme.unapply
    private val isUnapplySeq     = fun.symbol.name == nme.unapplySeq
    private def isBooleanUnapply = isUnapply && unapplyResultWithDummyUnapplySelector =:= BooleanTpe
    private def isRepeatedCaseClass = caseCtorParamTypes.exists(tpes => tpes.nonEmpty && isScalaRepeatedParamType(tpes.last))

    private def caseCtorParamTypes: Option[List[Type]] =
      if (isUnapply || isUnapplySeq) None else Some(fun.tpe.paramTypes)

    // TODO: the remainder needs to be reviewed regarding use of unapply-selector as a dummy argument,
    // on which the unapply method's result type may depend
    private def unapplyResultWithDummyUnapplySelector = fun.tpe.finalResultType

    private def resultOfGetInMonad = elementTypeFromGet(unapplyResultWithDummyUnapplySelector)

    // For a traditional extractor that returns an `Option[TupleN[..Ti..]]`, the component types `..Ti..`
    // Note, we do not unwrap a Tuple1... (similar for fromProductSelectors -- see pos/t796)
    private lazy val fromTupleComponents: Option[List[Type]] =
      resultOfGetInMonad match {
        case res if isTupleType(res) =>
          val components = tupleComponents(res)
          if (components.lengthCompare(1) > 0) Some(components)
          else None
        case _ => None
      }

    private lazy val fromProductSelectors: Option[List[Type]] = {
      val res = resultOfGetInMonad
      // Can't only check for _1 thanks to pos/t796.
      if (res.hasNonPrivateMember(nme._1) && res.hasNonPrivateMember(nme._2))
        Some(Stream.from(1).map(n => res.nonPrivateMember(newTermName("_" + n))).
             takeWhile(m => m.isMethod && m.paramLists.isEmpty).toList.map(m => res.memberType(m).resultType))
      else None
    }

    private lazy val booleanUnapply = if (isBooleanUnapply) Some(Nil) else None

    // In terms of the (equivalent -- if we're dealing with an unapply) case class, what are the constructor's parameter types?
    private val equivConstrParamTypes =
      caseCtorParamTypes orElse
      booleanUnapply orElse
      fromTupleComponents orElse
      fromProductSelectors getOrElse
      (resultOfGetInMonad :: Nil) // hope for the best


    // The non-sequence types which are extracted
    private val productTypes =
      if (equivConstrParamTypes.isEmpty) Nil
      else if (isUnapplySeq || (!isUnapply && isRepeatedCaseClass)) equivConstrParamTypes.init
      // scala/bug#9029 A pattern with arity-1 that doesn't match the arity of
      // the Product-like result of the `get` method, will match that result in its entirety.
      //
      // ```
      // warning: there was one deprecation warning; re-run with -deprecation for details
      // scala> object Extractor { def unapply(a: Any): Option[(Int, String)] = Some((1, "2")) }
      // defined object Extractor
      //
      // scala> "" match { case Extractor(x: Int, y: String) => }
      //
      // scala> "" match { case Extractor(xy : (Int, String)) => }
      // warning: there was one deprecation warning; re-run with -deprecation for details
      // ```
      else if (totalArity == 1 && equivConstrParamTypes.tail.nonEmpty) {
        warnPatternTupling()
        if (fromTupleComponents.nonEmpty) tupleType(equivConstrParamTypes) :: Nil
        else resultOfGetInMonad :: Nil
      }
      else equivConstrParamTypes

    private def notRepeated = (NoType, NoType, NoType)
    private val (elementType, sequenceType, repeatedType) =
      // case class C() is deprecated, but still need to defend against equivConstrParamTypes.isEmpty
      if (isUnapply || equivConstrParamTypes.isEmpty) notRepeated
      else {
        val lastParamTp = equivConstrParamTypes.last
        if (isUnapplySeq) {
          val elementTp =
            elementTypeFromHead(lastParamTp) orElse
            elementTypeFromApply(lastParamTp) orElse
            definitions.elementType(ArrayClass, lastParamTp)

          (elementTp, lastParamTp, scalaRepeatedType(elementTp))
        } else {
          definitions.elementType(RepeatedParamClass, lastParamTp) match {
            case NoType => notRepeated
            case elementTp => (elementTp, seqType(elementTp), lastParamTp)
          }
        }
      }

    private def err(msg: String) = context.error(fun.pos,msg)
    private def warn(msg: String) = context.warning(fun.pos,msg)
    private def depr(msg: String, since: String) = currentRun.reporting.deprecationWarning(fun.pos, fun.symbol.owner, msg, since)

    private def warnPatternTupling() =
      if (effectivePatternArity(args) == 1 && fromTupleComponents.nonEmpty) {
        val acceptMessage =
          if (equivConstrParamTypes contains NoType) ""
          else s" to hold ${equivConstrParamTypes.mkString("(", ", ", ")")}"
        val sym = fun.symbol.owner
        val arr = equivConstrParamTypes.length
        depr(s"${sym} expects $arr patterns$acceptMessage but crushing into $arr-tuple to fit single pattern (scala/bug#6675)", "2.11.0")
      }

    private def arityError(mismatch: String) = {
      val isErroneous = (productTypes contains NoType) && !(isSeq && (sequenceType ne NoType))

      val offeringString = if (isErroneous) "<error>" else productTypes match {
        case tps if isSeq => (tps.map(_.toString) :+ s"${elementType}*").mkString("(", ", ", ")")
        case Nil       => "Boolean"
        case tp :: Nil => tp
        case tps       => tps.mkString("(", ", ", ")")
      }
      val offerString = if (isErroneous) "" else s" offering $offeringString"
      val expected = (if (isSeq) "at least " else "") + productArity
      err(s"$mismatch patterns for ${fun.symbol.owner}$offerString: expected $expected, found $totalArity")
    }

    // emit error/warning on mismatch
    if (isStar && !isSeq) err("Star pattern must correspond with varargs or unapplySeq")
    else if (equivConstrParamTypes == List(NoType)) err(s"The result type of an ${fun.symbol.name} method must contain a member `get` to be used as an extractor pattern, no such member exists in ${unapplyResultWithDummyUnapplySelector}")
    else if (elementArity < 0) arityError("not enough")
    else if (elementArity > 0 && !isSeq) arityError("too many")
    else if (settings.warnStarsAlign && isSeq && productArity > 0 && elementArity > 0) warn(
      if (isStar) "Sequence wildcard (_*) does not align with repeated case parameter or extracted sequence; the result may be unexpected."
      else "A repeated case parameter or extracted sequence is not matched by a sequence wildcard (_*), and may fail at runtime.")

  }
}
