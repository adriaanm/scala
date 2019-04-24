object Example {
  sealed trait DBIOAction[+R, +S <: NoStream, -E <: Effect] {
    def flatMap[R2, S2 <: NoStream, E2 <: Effect](f: R => DBIOAction[R2, S2, E2]): DBIOAction[R2, S2, E with E2] = ???
    def map[R2](f: R => R2): DBIOAction[R2, NoStream, E] = ???
  }

  sealed trait NoStream
  sealed trait Streaming[+T] extends NoStream

  trait Effect
  object Effect {
    trait Write extends Effect
    trait Schema extends Effect
  }

  def createSchema(): DBIOAction[Unit, NoStream, Effect.Schema] = ???
  def writeAction(): DBIOAction[Option[Int], NoStream, Effect.Write] = ???

  def test: DBIOAction[Unit, _, _] = for {
    _ <- createSchema()
    _ <- writeAction()
  } yield ()
}
