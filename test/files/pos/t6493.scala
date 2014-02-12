class Test {
  def c: x.X forSome { val x : {type X <: C}; type C } = ???
  lazy val x = c // lazy vals are borked when dealing with existentials
}