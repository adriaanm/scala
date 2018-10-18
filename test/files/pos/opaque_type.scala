class Test {
  type T = T.T
  object T { self: {type T = Int} =>
    type T
    def lift(x: Int): T = x
  }

//  T.lift(1): Int // should error
  def res: T = T.lift(1) // result type should erase to Int, no boxing
}


/* NOK: TODO: proper error instead of exception

  object T { self: String {type TTTT = Int} =>  // no additional parent allowed
    type TTTT
  }

  object T { self: {type TTTT = Int} =>
    type TTTT <: String // should error due to incompat bound
  }


  object T { self: {type TTTT = Int; def foo: String} => // value member not allowed
  }

 */
