// if we disable deepBound, this fails because withTypeVars fails to solve the type vars
class Test {
  val c: x forSome { type x <: {type X <: C}; type C } = ???
  // error: type mismatch;
  // found   : Test.this.c.type (with underlying type x forSome { type x <: AnyRef{type X <: C}; type C })
  // required: x forSome { type x <: AnyRef{type X <: C}; type C }
  //  val c: x forSome { type x <: {type X <: C}; type C } = ???
  //      ^

  // val d: x#X forSome { type x <: {type X <: C}; type C } = ???
  // val e: x.X forSome { val x : {type X <: C}; type C } = ???
}