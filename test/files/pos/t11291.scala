class Test {
  def akkaResponseTimeLoggingFunction(a: String)(x: Int)(y: Boolean): Unit = ???

  // should compile without expected type
  akkaResponseTimeLoggingFunction("a") _
  akkaResponseTimeLoggingFunction("a")(_)(_)

  // eta-expansion only happens when there's an expected type (TODO: neg test for all of these w/o an expected type)
  akkaResponseTimeLoggingFunction("a") : (Int => Boolean => Unit)
  akkaResponseTimeLoggingFunction("a")(_) : (Int => Boolean => Unit)
  akkaResponseTimeLoggingFunction("a")(_)(_) : ((Int, Boolean) => Unit)

  // TODO what about this?
  akkaResponseTimeLoggingFunction(_)(1)(_)

}

