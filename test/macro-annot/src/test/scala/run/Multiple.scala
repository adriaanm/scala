import org.junit._
import org.junit.runner._
import org.junit.runners._
import Assert._

@RunWith(classOf[JUnit4])
class Multiple {
  @doubler @doubler class D

  @Test
  def multiple: Unit = {
    assertEquals((new DDDD).toString.take(4), "DDDD")
  }
}
