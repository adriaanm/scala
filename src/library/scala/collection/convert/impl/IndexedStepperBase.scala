/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala.collection.convert
package impl

import java.util.Spliterator

/** Abstracts all the generic operations of stepping over an indexable collection */
private[convert] abstract class IndexedStepperBase[Sub >: Null, Semi <: Sub](protected var i0: Int, protected var iN: Int)
  extends EfficientSubstep {
  protected def semiclone(half: Int): Semi

  def hasStep: Boolean = i0 < iN

  private[collection] def characteristics: Int = Spliterator.ORDERED + Spliterator.SIZED + Spliterator.SUBSIZED

  private[collection] def estimateSize: Long = iN - i0

  private[collection] def trySplit(): Sub = {
    if (iN-1 > i0) {
      val half = (i0+iN) >>> 1
      val ans = semiclone(half)
      i0 = half
      ans
    }
    else null
  }
}
