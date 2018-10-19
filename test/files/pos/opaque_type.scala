class Test {
  val x: T = ???
  type T = T.T
  object T { self: {type T = Int} =>
    type T
    def lift(x: Int): T = x
    def lower(x: T): Int = x
//    lower(x) // TODO: should type check
  }

//  T.lift(1): Int // compile error expected
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




object LogarithmTest {
  // The previous opaque type definition
  type Logarithm = Logarithm.Logarithm

  object Logarithm { self : {type Logarithm = Double} =>
    type Logarithm

    // These are the ways to lift to the logarithm type
    def apply(d: Double): Logarithm = math.log(d)

    def safe(d: Double): Option[Logarithm] =
      if (d > 0.0) Some(math.log(d)) else None

    // This is the first way to unlift the logarithm type
    def exponent(l: Logarithm): Double = l

    // Extension methods define opaque types' public APIs
    implicit class LogarithmOps(val `this`: Logarithm) extends AnyVal {
      // This is the second way to unlift the logarithm type
      def toDouble: Double = math.exp(`this`)
      def +(that: Logarithm): Logarithm = apply(math.exp(`this`) + math.exp(that)) // TODO: can we make it work when writing `Logarithm` instead of `apply`?
      def *(that: Logarithm): Logarithm = apply(`this` + that)
    }
  }
}
