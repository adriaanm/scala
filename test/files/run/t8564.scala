object Test extends App {
  def shouldPrint(x: AnyRef): x.type = { println(x); x }
  // the argument of the apply is typed as a ConstantType,
  // but the Apply expression should have type String (or, after SIP-23, "test".type)
  // the bug was that Apply would be constant folded to the corresponding constant
  // (skipping the apply altogether)
  shouldPrint("Hello? Is anybody there??")
}