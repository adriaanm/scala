import scala.tools.partest.{ReplTest, Hashless}

/* the ambiguity in the repl is consistent with the regular compiler:
class AmbiguousReferences {
  object x { def x = () }
  { // import scala.tools.nsc.interpreter.`{{` // this bumps the nesting level into ambiguity
    import x._

    { // import scala.tools.nsc.interpreter.`{{`
      x

      { // import scala.tools.nsc.interpreter.`{{`
        x

      }
    }
  }
}
*/
object Test extends ReplTest with Hashless {
  def code = """
object x { def x = () }
import x._
x
x
  """
}
