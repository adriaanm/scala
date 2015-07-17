package scala.reflect.internal.interactive

import scala.sys.BooleanProp

/**
 * Created by adriaan on 7/17/15.
 */
abstract class InteractiveProps {
  def debug: BooleanProp
  def trace: BooleanProp

  def isPaged: Boolean
  def isAcross: Boolean
}
