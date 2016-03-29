object p { def X = "p" }
object q { def X = "q" }

object Test extends App {
  import p.X
  import scala.tools.nsc.interpreter.`{{`
  import q.X
  println(X)
}
