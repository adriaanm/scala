object ClashOverloadNoSig {
  // error: overloaded method apply needs result type
  private def apply(x: Int) = if (x > 0) new ClashOverloadNoSig(x) else apply("")
  def apply(x: String): ClashOverloadNoSig = ???
}
// TODO: suppress this one?
// error: method apply is defined twice; conflicting symbols both originated in file 'userdefined_apply.scala'
case class ClashOverloadNoSig private (x: Int)

object ClashRecNoSig {
  // TODO: should be "recursive method needs signature"
  // error: overloaded method apply needs result type
  private def apply(x: Int) = if (x > 0) ClashRecNoSig(1) else ???
}
case class ClashRecNoSig private (x: Int)

object NoClashNoSig {
 // error: overloaded method apply needs result type
 private def apply(x: Boolean) = if (x) NoClashNoSig(1) else ???
}
case class NoClashNoSig private (x: Int)

object NoClashOverload {
  // error: overloaded method apply needs result type
  private def apply(x: Boolean) = if (x) NoClashOverload(1) else apply("")
  def apply(x: String): NoClashOverload = ???
}
case class NoClashOverload private (x: Int)
