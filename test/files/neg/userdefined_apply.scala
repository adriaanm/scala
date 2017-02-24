// NOTE: the companion inherits a public apply method from Function1!
case class NeedsCompanion private (x: Int)

object ClashNoSig { // ok
  private def apply(x: Int) = if (x > 0) new ClashNoSig(x) else ???
}
case class ClashNoSig private (x: Int)

// needs sig because overloaded -- TODO: actual error is double def
object ClashOverloadNoSig {
  private def apply(x: Int) = if (x > 0) new ClashOverloadNoSig(x) else apply("")
  def apply(x: String): ClashOverloadNoSig = ???
}
case class ClashOverloadNoSig private (x: Int)

// needs sig because it's recursive
object ClashSigRec {
  private def apply(x: Int) = if (x > 0) ClashSigRec(1) else ???
}
case class ClashSigRec private (x: Int)

// needs sig because of overloading
object NoClashNoSig {
 private def apply(x: Boolean) = if (x) NoClashNoSig(1) else ???
}
case class NoClashNoSig private (x: Int)

object NoClashOverload {
  // needs full sig
  private def apply(x: Boolean) = if (x) NoClashOverload(1) else apply("")
  def apply(x: String): NoClashOverload = ???
}
case class NoClashOverload private (x: Int)
