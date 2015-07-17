/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author Paul Phillips
 */

package scala.reflect.internal.interactive

import Completion._

/** A stateful factory for ScalaCompleters
  * The factory itself should be cheap to initialize (no side-effects on compiler!)
  */
trait Completion {
  def resetVerbosity(): Unit
  // will be called after repl is initialized
  def completer(): ScalaCompleter
}

object Completion {
  case class Candidates(cursor: Int, candidates: List[String]) { }
  val NoCandidates = Candidates(-1, Nil)

  trait ScalaCompleter {
    def complete(buffer: String, cursor: Int): Candidates
  }

  def looksLikeInvocation(code: String) = (
        (code != null)
    &&  (code startsWith ".")
    && !(code == ".")
    && !(code startsWith "./")
    && !(code startsWith "..")
  )
  object Forwarder {
    def apply(forwardTo: () => Option[CompletionAware]): CompletionAware = new CompletionAware {
      def completions(verbosity: Int) = forwardTo() map (_ completions verbosity) getOrElse Nil
      override def follow(s: String) = forwardTo() flatMap (_ follow s)
    }
  }
}
