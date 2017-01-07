import scala.runtime.ControlThrowable

object Test {
  def main(args: Array[String]) {
    try {
      println("...")
    }
    finally {
      try {
        println("...")
      }
      finally {
        try {
          println("...")
        }
        catch {
          case ct: ControlThrowable => throw(ct)
          case t: Throwable => t.printStackTrace()
        }
      }
    }
  }
}
