/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author Stepan Koltsov
 */

package scala.reflect.internal.interactive

import session.History

/** Reads lines from an input stream */
trait InteractiveReader {
  def postInit(): Unit = {}

  val interactive: Boolean

  def reset(): Unit
  def history: History
  def completions: List[Completion]
  def redrawLine(): Unit

  def readYesOrNo(prompt: String, alt: => Boolean): Boolean = readOneKey(prompt) match {
    case 'y'  => true
    case 'n'  => false
    case -1   => false // EOF
    case _    => alt
  }

  protected def readOneLine(prompt: String): String
  protected def readOneKey(prompt: String): Int

  def readLine(prompt: String): String = readOneLine(prompt)
}

