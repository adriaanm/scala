/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author Paul Phillips
 */

package scala.tools.nsc.interpreter.jline

import scala.reflect.internal.interactive.session.SimpleHistory

import java.util.{Iterator => JIterator, ListIterator => JListIterator}

import _root_.jline.{console => jconsole}
import jconsole.history.History.{Entry => JEntry}

import scala.reflect.io.{File, Path}
import scala.util.Properties.{propOrNone, userHome}

abstract class JLineFileHistory extends SimpleHistory with JLineFileBackedHistory {
  //   val ContinuationChar = '\003'
  //   val ContinuationNL: String = Array('\003', '\n').mkString

  final val defaultFileName = ".scala_history"

  def defaultFile: File = File(
    propOrNone("scala.shell.histfile") map (Path(_))
      getOrElse (Path(userHome) / defaultFileName))

  override def historicize(text: String): Boolean = {
    text.lines foreach add
    moveToEnd()
    true
  }

  override def add(item: CharSequence): Unit = {
    if (!isEmpty && last == item)
      debug("Ignoring duplicate entry '" + item + "'")
    else {
      super.add(item)
      addLineToFile(item)
    }
  }
  override def toString = "History(size = " + size + ", index = " + index + ")"

  import scala.collection.JavaConverters._

  override def asStrings(from: Int, to: Int): List[String] =
    entries(from).asScala.take(to - from).map(_.value.toString).toList

  case class Entry(index: Int, value: CharSequence) extends JEntry {
    override def toString = value.toString
  }

  private def toEntries(): Seq[JEntry] = buf.zipWithIndex map { case (x, i) => Entry(i, x)}
  def entries(idx: Int): JListIterator[JEntry] = toEntries().asJava.listIterator(idx)
  def entries(): JListIterator[JEntry] = toEntries().asJava.listIterator()
  def iterator: JIterator[JEntry] = toEntries().iterator.asJava
}
