class Test {
  def c: T forSome { type T <: C {def foo: Int}; type C } = ???
  lazy val x = c // lazy vals are borked when dealing with existentials
}