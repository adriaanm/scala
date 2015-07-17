/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author Paul Phillips
 */

package scala.reflect.internal.interactive.session

/** An implementation-agnostic history interface which makes no
 *  reference to the jline classes.  Very sparse right now.
 */
trait History {
  protected def debug(msg: String): Unit = {}

  def historicize(text: String): Boolean = false

  def asStrings: List[String]
  def asStrings(from: Int, to: Int): List[String] = asStrings.slice(from, to)
  def index: Int
  def size: Int
}
object NoHistory extends History {
  def asStrings       = Nil
  def index           = 0
  def size            = 0
}
