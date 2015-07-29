/* NSC -- new scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author  Martin Odersky
 */

package scala
package reflect
package internal

import scala.collection.{ mutable, immutable }

/** The name of this trait defines the eventual intent better than
 *  it does the initial contents.
 */
trait ExistentialsAndSkolems {
  self: SymbolTable =>

  /** Map a list of type parameter symbols to skolemized symbols, which
   *  can be deskolemized to the original type parameter. (A skolem is a
   *  representation of a bound variable when viewed inside its scope.)
   *  !!!Adriaan: this does not work for hk types.
   *
   *  Skolems will be created at level 0, rather than the current value
   *  of `skolemizationLevel`. (See SI-7782)
   */
  def deriveFreshSkolems(tparams: List[Symbol]): List[Symbol] = {
    class Deskolemizer extends LazyType {
      override val typeParams = tparams
      val typeSkolems  = typeParams map (_.newTypeSkolem setInfo this)
      override def complete(sym: Symbol) {
        // The info of a skolem is the skolemized info of the
        // actual type parameter of the skolem
        sym setInfo sym.deSkolemize.info.substSym(typeParams, typeSkolems)
      }
    }

    val saved = skolemizationLevel
    skolemizationLevel = 0
    try new Deskolemizer().typeSkolems
    finally skolemizationLevel = saved
  }

  def isRawParameter(sym: Symbol) = // is it a type parameter leaked by a raw type?
    sym.isTypeParameter && sym.owner.isJavaDefined

  /** Given a set `rawSyms` of term- and type-symbols, and a type
   *  `tp`, produce a set of fresh type parameters and a type so that
   *  it can be abstracted to an existential type. Every type symbol
   *  `T` in `rawSyms` is mapped to a clone. Every term symbol `x` of
   *  type `T` in `rawSyms` is given an associated type symbol of the
   *  following form:
   *
   *    type x.type <: T with Singleton
   *
   *  The name of the type parameter is `x.type`, to produce nice
   *  diagnostics. The Singleton parent ensures that the type
   *  parameter is still seen as a stable type. Type symbols in
   *  rawSyms are fully replaced by the new symbols. Term symbols are
   *  also replaced, except for term symbols of an Ident tree, where
   *  only the type of the Ident is changed.
   */
  final def existentialTransform[T](rawSyms: List[Symbol], tp: Type, rawOwner: Symbol = NoSymbol)(creator: (List[Symbol], Type) => T): T = {
    def newOwner(sym: Symbol) = 
      if (isRawParameter(sym)) rawOwner orElse abort(s"no owner provided for existential transform over raw parameter: $sym")
      else sym.owner

    val freshSyms = deriveSymbols(rawSyms, sym => sym.existentialSym(newOwner(sym)))
    val freshTp   = tp.substSym(rawSyms, freshSyms)
    // println(s"existentialTransform: rawSyms = $rawSyms")
    // println(s"existentialTransform: freshSyms = ${freshSyms.map(_.defString)} / freshTp = $freshTp")
    creator(freshSyms, freshTp)
  }

  /**
   * Compute an existential type from hidden symbols `hidden` and type `tp`.
   * @param hidden   The symbols that will be existentially abstracted
   * @param hidden   The original type
   * @param rawOwner The owner for Java raw types.
   */
  final def packSymbols(hidden: List[Symbol], tp: Type, rawOwner: Symbol = NoSymbol): Type =
    if (hidden.isEmpty) tp
    else existentialTransform(hidden, tp, rawOwner)(existentialAbstraction)
}
