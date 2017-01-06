/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala

import scala.runtime.SystemProperties

/** The package object `scala.sys` contains methods for reading
 *  and altering core aspects of the virtual machine as well as the
 *  world outside of it.
 *
 *  @author Paul Phillips
 *  @version 2.9
 *  @since   2.9
 */
object sys {
  /** Throw a new RuntimeException with the supplied message.
   *
   *  @return   Nothing.
   */
  def error(message: String): Nothing = throw new RuntimeException(message)

  /** Exit the JVM with the default status code.
   *
   *  @return   Nothing.
   */
  def exit(): Nothing = exit(0)

  /** Exit the JVM with the given status code.
   *
   *  @return   Nothing.
   */
  def exit(status: Int): Nothing = {
    java.lang.System.exit(status)
    throw new Throwable()
  }

  /** A bidirectional, mutable Map representing the current system Properties.
   *
   *  @return   a SystemProperties.
   *  @see      [[SystemProperties]]
   */
  def props: SystemProperties = new SystemProperties
}
