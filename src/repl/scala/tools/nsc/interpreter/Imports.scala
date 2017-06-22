/* NSC -- new Scala compiler
 * Copyright 2005-2016 LAMP/EPFL
 * @author Paul Phillips
 */
package scala.tools.nsc
package interpreter

import scala.collection.mutable

trait Imports {
  self: IMain =>

  import global.{Type, Tree, Import, ImportSelector, Select, Ident, newTermName, Symbol, TermSymbol, NoType, Name, enteringPickler}
  import global.definitions.{ ScalaPackage, JavaLangPackage, PredefModule }
  import memberHandlers._

  /** Synthetic import handlers for the language defined imports. */
  private def makeWildcardImportHandler(sym: Symbol): ImportHandler = {
    val hd :: tl = sym.fullName.split('.').toList map newTermName
    val tree = Import(
      tl.foldLeft(Ident(hd): Tree)((x, y) => Select(x, y)),
      ImportSelector.wildList
    )
    tree setSymbol sym
    new ImportHandler(tree)
  }

  /** Symbols whose contents are language-defined to be imported. */
  private def languageWildcardSyms: List[Symbol] = List(JavaLangPackage, ScalaPackage, PredefModule)
  def languageWildcardHandlers = languageWildcardSyms map makeWildcardImportHandler

  /** Tuples of (source, imported symbols) in the order they were imported.
   */
  private def importedSymbolsBySource: List[(Symbol, List[Symbol])] = {
    val lang    = languageWildcardSyms map (sym => (sym, membersAtPickler(sym)))
    val session = importHandlers filter (_.targetType != NoType) map { mh =>
      (mh.targetType.typeSymbol, mh.importedSymbols)
    }

    lang ++ session
  }
  def implicitSymbolsBySource: List[(Symbol, List[Symbol])] = {
    importedSymbolsBySource map {
      case (k, vs) => (k, vs filter (_.isImplicit))
    } filterNot (_._2.isEmpty)
  }

  private def allReqAndHandlers =
    prevRequestList flatMap (req => req.handlers map (req -> _))

  private def membersAtPickler(sym: Symbol): List[Symbol] =
    enteringPickler(sym.info.nonPrivateMembers.toList)
}
