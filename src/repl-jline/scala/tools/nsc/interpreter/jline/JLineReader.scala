/** NSC -- new Scala compiler
  *
  * Copyright 2005-2015 LAMP/EPFL
  * @author Stepan Koltsov
  * @author Adriaan Moors
  */

package scala.tools.nsc.interpreter.jline

import java.io.IOException

import scala.reflect.internal.interactive
import scala.reflect.internal.interactive.Completion.ScalaCompleter
import scala.reflect.internal.interactive.{InteractiveProps, Completion}
import scala.reflect.internal.interactive.session.{SimpleHistory, History}

import scala.reflect.internal.util.VariColumnTabulator

import java.util.{Collection => JCollection, List => JList}

import _root_.jline.{console => jconsole}
import jline.console.completer.{ArgumentCompleter, Completer}
import jconsole.history.{History => JHistory}

/**
 * Reads from the console using JLine.
 *
 * Eagerly instantiates all relevant JLine classes, so that we can detect linkage errors on `new JLineReader` and retry.
 */
class InteractiveReader(val replProps: InteractiveProps, val completions: List[Completion]) extends interactive.InteractiveReader {
  val interactive = true

  trait HistoryDebugging extends History {
    override def debug(msg: String): Unit = if (replProps.debug || replProps.trace) println(msg)
  }

  val history: History =
    try new JLineFileHistory with HistoryDebugging
    catch { case x: Exception => new SimpleHistory with HistoryDebugging }


  private val consoleReader = {
    val reader = new JLineConsoleReader

    reader setPaginationEnabled replProps.isPaged

    // ASAP
    reader setExpandEvents false

    reader setHistory history.asInstanceOf[JHistory]

    reader
  }

  override def postInit() = {
    consoleReader.initCompletion(completions)
  }

  def reset() = consoleReader.reset()

  def redrawLine() = consoleReader.redrawLineAndFlush()

  def readOneLine(prompt: String) = consoleReader.readLine(prompt)

  def readOneKey(prompt: String) = consoleReader.readOneKey(prompt)

  class JLineConsoleReader extends jconsole.ConsoleReader with VariColumnTabulator {
    val marginSize = 3

    def isAcross = replProps.isAcross
    def width    = getTerminal.getWidth()
    def height   = getTerminal.getHeight()

    private def morePrompt = "--More--"

    private def emulateMore(): Int = {
      val key = readOneKey(morePrompt)
      try key match {
        case '\r' | '\n' => 1
        case 'q' => -1
        case _ => height - 1
      }
      finally {
        eraseLine()
        // TODO: still not quite managing to erase --More-- and get
        // back to a scala prompt without another keypress.
        if (key == 'q') {
          putString(getPrompt())
          redrawLine()
          flush()
        }
      }
    }

    override def printColumns(items: JCollection[_ <: CharSequence]): Unit = {
      import scala.collection.JavaConverters._
      printColumns_(items.asScala.toList map ("" + _))
    }

    private def printColumns_(items: List[String]): Unit = if (items exists (_ != "")) {
      val grouped = tabulate(items)
      var linesLeft = if (isPaginationEnabled()) height - 1 else Int.MaxValue
      grouped foreach { xs =>
        println(xs.mkString)
        linesLeft -= 1
        if (linesLeft <= 0) {
          linesLeft = emulateMore()
          if (linesLeft < 0)
            return
        }
      }
    }

    def readOneKey(prompt: String) = {
      this.print(prompt)
      this.flush()
      this.readCharacter()
    }

    private val msgEINTR = "Interrupted system call"
    private def restartSysCalls[R](body: => R, reset: => Unit): R =
      try body catch {
        case e: IOException if e.getMessage == msgEINTR => reset; body
      }

    def reset() = getTerminal().reset()

    override def readLine(prompt: String) =
    // hack necessary for OSX jvm suspension because read calls are not restarted after SIGTSTP
      if (scala.util.Properties.isMac) restartSysCalls(super.readLine(prompt), reset())
      else super.readLine(prompt)

    def eraseLine() = resetPromptLine("", "", 0)

    def redrawLineAndFlush(): Unit = {
      flush()
      drawLine()
      flush()
    }

    class JLineCompleter(completer: ScalaCompleter) extends Completer {
      def complete(_buf: String, cursor: Int, candidates: JList[CharSequence]): Int = {
        import Completion.Candidates

        val buf = if (_buf == null) "" else _buf
        val Candidates(newCursor, newCandidates) = completer.complete(buf, cursor)
        newCandidates foreach (candidates add _)
        newCursor
      }
    }

    // A hook for running code after the repl is done initializing.
    def initCompletion(completions: List[Completion]): Unit = {
      this setBellEnabled false

      completions foreach { completion =>
        val argumentCompleter = new ArgumentCompleter(new JLineDelimiter, new JLineCompleter(completion.completer()))
        argumentCompleter setStrict false
        this addCompleter argumentCompleter
      }

      if (completions.nonEmpty)
        this setAutoprintThreshold 400 // max completion candidates without warning
    }
  }

}
