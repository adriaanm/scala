import scala.tools.partest.Util.trace


object Test extends App {
  def intercept = allCatch.withApply(_.getClass)
  val t: Boolean = true
  trace(if (t) -0d else 0d)
  trace(if (t) 0d else -0d)
  trace(intercept(if (???) -0d else 0d))
  trace(intercept(if (???) 0d else 0d))
  trace(intercept(if (???) () else ()))
}
