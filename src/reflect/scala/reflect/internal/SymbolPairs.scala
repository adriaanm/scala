/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala
package reflect
package internal

import scala.collection.mutable
import util.HashSet
import scala.annotation.tailrec
import scala.collection.immutable.BitSet

/** An abstraction for considering symbol pairs.
 *  One of the greatest sources of compiler bugs is that symbols can
 *  trivially lose their prefixes and turn into some completely different
 *  type with the smallest of errors. It is the exception not the rule
 *  that type comparisons are done correctly.
 *
 *  This offers a small step toward coherence with two abstractions
 *  which come up over and over again:
 *
 *    RelativeTo: operations relative to a prefix
 *    SymbolPair: two symbols being related somehow, plus the class
 *       in which the relation is being performed
 *
 *  This is only a start, but it is a start.
 */
abstract class OverridingPairs {
  val global: SymbolTable
  import global._

  final case class SymbolPair(base: Symbol, low: Symbol, high: Symbol) {
    private[this] val self  = base.thisType

    def pos                 = if (low.owner == base) low.pos else if (high.owner == base) high.pos else base.pos
    def rootType: Type      = self

    def lowType: Type       = self memberType low
    def lowErased: Type     = erasure.specialErasure(base)(low.tpe)
    def lowClassBound: Type = classBoundAsSeen(low.tpe.typeSymbol)

    def highType: Type       = self memberType high
    def highInfo: Type       = self memberInfo high
    def highErased: Type     = erasure.specialErasure(base)(high.tpe)
    def highClassBound: Type = classBoundAsSeen(high.tpe.typeSymbol)

    def isErroneous = low.tpe.isErroneous || high.tpe.isErroneous
    def sameKind    = sameLength(low.typeParams, high.typeParams)

    private def classBoundAsSeen(tsym: Symbol) =
      tsym.classBound.asSeenFrom(rootType, tsym.owner)

    private def memberDefString(sym: Symbol, where: Boolean) = {
      val def_s = (
        if (sym.isConstructor) s"$sym: ${self memberType sym}"
        else sym defStringSeenAs (self memberType sym)
      )
      def_s + whereString(sym)
    }
    /** A string like ' at line 55' if the symbol is defined in the class
     *  under consideration, or ' in trait Foo' if defined elsewhere.
     */
    private def whereString(sym: Symbol) =
      if (sym.owner == base) " at line " + sym.pos.line else sym.locationString

    def lowString  = memberDefString(low, where = true)
    def highString = memberDefString(high, where = true)

    override def toString = sm"""
      |Cursor(in $base) {
      |   high  $highString
      | erased  $highErased
      |  infos  ${high.infosString}
      |    low  $lowString
      | erased  $lowErased
      |  infos  ${low.infosString}
      |}""".trim
  }

  /** The cursor class
   *  @param base   the base class containing the participating symbols
   */
  sealed class Cursor(val base: Symbol) { cursor =>
    /** A symbol for which exclude returns true will not appear as
      * either end of a pair.
      *
      * By default, these are constructors and private/artifact symbols, including bridges.
      */
    protected def exclude(sym: Symbol): Boolean =
      ( sym.isPrivateLocal
      || sym.isArtifact
      || sym.isConstructor
      || (sym.isPrivate && sym.owner != base) // Privates aren't inherited. Needed for pos/t7475a.scala
      )

    /** Does `low` match `high` such that (low, high) should be
      * considered as a pair? Types always match. Term symbols
      * match if their member types relative to `self` match.
      *
      * Overridden in DoubleDefsCursor to just `!high.isPrivate`
      */
    protected def matches =
      low.isType ||
      ((low.owner != high.owner) // don't try to form pairs from overloaded members
       && !high.isPrivate // private or private[this] members never are overridden -- TODO we don't call exclude(high), should we?
       && !exclude(low) // this admits private, as one can't have a private member that matches a less-private member.
       && (lowMemberType matches highMemberType)
      )

    /** Overridden in BridgesCursor. */
    protected def filterParents(parents: List[Type]): List[Type] = parents


    /** The scope entries that have already been visited as highSymbol
     *  (but may have been excluded via hasCommonParentAsSubclass.)
     *  These will not appear as lowSymbol.
     */
    private[this] val visited = HashSet[ScopeEntry]("visited", 64)

    /** All the symbols which can take part in a pair.
      *
      * Initialization has to run now so decls is populated before
      * the declaration of curEntry.
      */
    private[this] val decls: Scope = computeDecls()

    // The current low and high symbols; the high may be null.
    // Whenever lowSymbol is changed, lowMemberTypeCache should be nulled
    private[this] var lowSymbol: Symbol  = _
    private[this] var highSymbol: Symbol = _

    private[this] var lowMemberTypeCache: Type = _
    def lowMemberType: Type = {
      if (lowMemberTypeCache eq null)
        lowMemberTypeCache = base.thisType.memberType(lowSymbol)

      lowMemberTypeCache
    }

    def highMemberType: Type = base.thisType.memberType(highSymbol)

    // The current entry candidates for low and high symbol.
    private[this] var curEntry  = decls.elems
    private[this] var nextEntry = curEntry

    private[this] val subParentsCache: mutable.AnyRefMap[Symbol, BitSet] =
      perRunCaches.newAnyRefMap[Symbol, BitSet]()

    private[this] val parentClasses =
      filterParents(base.info.parents).toArray.map(_.typeSymbol)//.filterNot(p => p.isTrait)

    // These fields are initially populated with a call to next().
    next()


    // populate the above data structures
    private def computeDecls(): Scope = {
      val decls = newScope

      // Fill `decls` with lower symbols shadowing higher ones
      def fillDecls(include: Symbol => Boolean)(bc: Symbol): Unit =
        bc.info.decls foreach { sym => if (include(sym) && !exclude(sym)) decls enter sym }

      val bases = base.info.baseClasses.toArray

      // first, deferred (this will need to change if we change lookup rules!)
      bases.reverseIterator foreach fillDecls(_.initialize.isDeferred)

      // then, concrete.
      bases.reverseIterator foreach fillDecls(sym => !sym.isDeferred) // symbols will already initialized

      decls
    }

    /** Map `base` to a bitset that indicates for each index in `parents`
      * whether that parent is a non-trait subclass of `base`.
      *
      *   i \in subParents(b) iff
      *   exists b \in bases:
      *     parents(i) isNonBottomSubClass b
      */
    private def subParents(bc: Symbol): BitSet = {
      def compute =
        if (parentClasses.isEmpty) BitSet.empty
        else {
          val idxs = (0L until parentClasses.size.toLong).iterator
          BitSet.fromBitMaskNoCopy(idxs.filter(i => parentClasses(i.toInt).isNonBottomSubClass(bc)).toArray)
        }

      subParentsCache.getOrElseUpdate(bc, compute)
    }

    // For **non-trait** classes C and D, C isSubclass D implies linearisation(D) linearisation(C)
    private def shareLinearisationSuffix(cls1: Symbol, cls2: Symbol): Boolean = {
      val res = (subParents(cls1) intersect subParents(cls2)).nonEmpty
      if (res) {
        println(s"shareLinearisationSuffix $cls1 $cls2 ${(subParents(cls1) intersect subParents(cls2)).toSet.map(parentClasses.applyOrElse[Int, Symbol](_, _ => NoSymbol))}")
      }
      res
    }

    @tailrec private def advanceNextEntry(): Unit = {
      if (nextEntry ne null) {
        nextEntry = decls lookupNextEntry nextEntry
        if (nextEntry ne null) {
          highSymbol = nextEntry.sym
          val isMatch = matches && { visited addEntry nextEntry ; true } // side-effect visited on all matches

          // skip nextEntry if a class in `parents` is a subclass of the
          // owners of both low and high.
          if (!isMatch || shareLinearisationSuffix(lowSymbol.owner, highSymbol.owner))
            advanceNextEntry()
        }
      }
    }
    @tailrec private def advanceCurEntry(): Unit = {
      if (curEntry ne null) {
        curEntry = curEntry.next
        if (curEntry ne null) {
          if (visited(curEntry) || exclude(curEntry.sym))
            advanceCurEntry()
          else
            nextEntry = curEntry
        }
      }
    }

    /** The `low` and `high` symbol.  In the context of overriding pairs,
     *  low == overriding and high == overridden.
     */
    def low  = lowSymbol
    def high = highSymbol

    def hasNext     = curEntry ne null
    def currentPair = new SymbolPair(base, low, high)
    def iterator: Iterator[SymbolPair] = new collection.AbstractIterator[SymbolPair] {
      def hasNext = cursor.hasNext
      def next()  = try cursor.currentPair finally cursor.next()
    }

    // Note that next is called once during object initialization to
    // populate the fields tracking the current symbol pair.
    @tailrec final def next(): Unit = {
      if (curEntry ne null) {
        lowSymbol = curEntry.sym
        lowMemberTypeCache = null

        advanceNextEntry()        // sets highSymbol
        if (nextEntry eq null) {
          advanceCurEntry()
          next()
        }
      }
    }
  }

  // Used to compute bridges during erasure (hence, only the first parent is relevant, and we exclude everything but methods)
  final class BridgesCursor(root: Symbol) extends Cursor(root) {
    override protected def filterParents(parents: List[Type]) = parents.take(1)
    // Varargs bridges may need generic bridges due to the non-repeated part of the signature of the involved methods.
    // The vararg bridge is generated during refchecks (probably to simplify override checking),
    // but then the resulting varargs "bridge" method may itself need an actual erasure bridge.
    // TODO: like javac, generate just one bridge method that wraps Seq <-> varargs and does erasure-induced casts
    override protected def exclude(sym: Symbol) = !sym.isMethod || super.exclude(sym)
  }

  // Used during erasure to check for double definitions, so matching is redefined to just hide private members on the high side
  final class DoubleDefsCursor(root: Symbol, refChecksId: Phase#Id) extends Cursor(root) {
    override def exclude(sym: Symbol): Boolean =
      ( sym.isType
        || super.exclude(sym)
        // specialized members have no type history before 'specialize', causing double def errors for curried defs
        // don't use hasTypeAt(refChecks), since that also catches other late-synthesized members (fields/objects)
        || !sym.hasTypeAt(refChecksId)) //sym.hasFlag(Flags.SPECIALIZED))

    override def matches = !high.isPrivate
  }

}
