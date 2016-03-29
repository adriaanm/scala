// Copyright 2017 LAMP/EPFL and Lightbend, Inc.
package scala.tools.nsc.interpreter

/** A magic symbol that, when imported, bumps the effective nesting level
  * of the typechecker.
  *
  * The REPL inserts this import to control scoping in code templates,
  * without excessive lexical noise.
  *
  * ```
  *  import p.X
  *  import scala.tools.nsc.interpreter.`{{`
  *  import q.X
  *  X           // q.X
  * ```
  *
  * Its name is chosen to suggest scoping by braces; the brace is doubled
  * to avoid confusion in printed output, as the name will be visible to
  * a REPL user inspecting generated code.
  *
  * There is no complementary symbol to restore the nesting level.
  */
object `{{`
