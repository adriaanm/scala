import scala.reflect.internal.util
import scala.reflect.internal.util.ShutdownHookThread

object Test {
  ShutdownHookThread {
    Thread.sleep(1000)
    println("Test#shutdown.")
  }

  def daemon() = {
    val t = new Thread {
      override def run() {
        Thread.sleep(10000)
        println("Hallelujah!") // should not see this
      }
    }
    t.setDaemon(true)
    t.start()
    t
  }

  def nonDaemon() = {
    val t = new Thread {
      override def run() {
        Thread.sleep(100)
        println("Fooblitzky!")
      }
    }
    t.start()
    t
  }

  def main(args: Array[String]): Unit = {
    daemon()
    nonDaemon()
    util.ShutdownHookThread {
      println("main#shutdown.")
    }
  }
}
