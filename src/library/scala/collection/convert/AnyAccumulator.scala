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

import scala.collection.{Factory, mutable}
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
 * An `AnyAccumulator` is a low-level collection specialized for gathering
 * elements in parallel and then joining them in order by merging Accumulators.
 * Accumulators can contain more than `Int.MaxValue` elements.
 *
 * TODO: doc performance characteristics.
 */
final class AnyAccumulator[A]
  extends Accumulator[A, AnyAccumulator, AnyAccumulator[A]]
    with collection.IterableOps[A, AnyAccumulator, AnyAccumulator[A]] {
  // Elements are added to `current`. Once full, it's added to `history`, and a new `current` is
  // created with `nextBlockSize` (which depends on `totalSize`).
  // `cumul(i)` is `(0 until i).map(history(_).length)`
  private[convert] var current: Array[AnyRef] = AnyAccumulator.emptyAnyRefArray
  private[convert] var history: Array[Array[AnyRef]] = AnyAccumulator.emptyAnyRefArrayArray
  private[convert] var cumul: Array[Long] = AnyAccumulator.emptyLongArray

  private[convert] def cumulative(i: Int): Long = cumul(i)

  private def expand(): Unit = {
    if (index > 0) {
      if (hIndex >= history.length) hExpand()
      history(hIndex) = current
      cumul(hIndex) = (if (hIndex > 0) cumulative(hIndex-1) else 0) + index
      hIndex += 1
    }
    current = new Array[AnyRef](nextBlockSize)
    index = 0
  }

  private def hExpand(): Unit = {
    if (hIndex == 0) {
      history = new Array[Array[AnyRef]](4)
      cumul = new Array[Long](4)
    }
    else {
      history = java.util.Arrays.copyOf(history, history.length << 1)
      cumul = java.util.Arrays.copyOf(cumul, cumul.length << 1)
    }
  }

  /** Appends an element to this `AnyAccumulator`. */
  final def addOne(a: A): this.type = {
    totalSize += 1
    if (index >= current.length) expand()
    current(index) = a.asInstanceOf[AnyRef]
    index += 1
    this
  }

  /** Result collection consisting of all elements appended so far. */
  override def result(): AnyAccumulator[A] = this

  /** Removes all elements from `that` and appends them to this `AnyAccumulator`. */
  final def drain[A1 <: A](that: AnyAccumulator[A1]): Unit = {
    var h = 0
    var prev = 0L
    var more = true
    while (more && h < that.hIndex) {
      val n = (that.cumulative(h) - prev).toInt
      if (current.length - index >= n) {
        System.arraycopy(that.history(h), 0, current, index, n)
        prev = that.cumulative(h)
        index += n
        h += 1
      }
      else more = false
    }
    if (h >= that.hIndex && current.length - index >= that.index) {
      if (that.index > 0) System.arraycopy(that.current, 0, current, index, that.index)
      index += that.index
    }
    else {
      val slots = (if (index > 0) 1 else 0) + that.hIndex - h
      if (hIndex + slots > history.length) {
        val n = math.max(4, 1 << (32 - java.lang.Integer.numberOfLeadingZeros(1 + hIndex + slots)))
        history = java.util.Arrays.copyOf(history, n)
        cumul = java.util.Arrays.copyOf(cumul, n)
      }
      var pv = if (hIndex > 0) cumulative(hIndex-1) else 0L
      if (index > 0) {
        pv += index
        cumul(hIndex) = pv
        history(hIndex) = if (index < (current.length >>> 3) && current.length > 32) java.util.Arrays.copyOf(current, index) else current
        hIndex += 1
      }
      while (h < that.hIndex) {
        pv += that.cumulative(h) - prev
        prev = that.cumulative(h)
        cumul(hIndex) = pv
        history(hIndex) = that.history(h)
        h += 1
        hIndex += 1
      }
      index = that.index
      current = that.current
    }
    totalSize += that.totalSize
    that.clear()
  }

  override def clear(): Unit = {
    super.clear()
    current = AnyAccumulator.emptyAnyRefArray
    history = AnyAccumulator.emptyAnyRefArrayArray
    cumul  = AnyAccumulator.emptyLongArray
  }

  /** Retrieves the `ix`th element. */
  final def apply(ix: Long): A = {
    if (totalSize - ix <= index || hIndex == 0) current((ix - (totalSize - index)).toInt).asInstanceOf[A]
    else {
      val w = seekSlot(ix)
      history((w >>> 32).toInt)((w & 0xFFFFFFFFL).toInt).asInstanceOf[A]
    }
  }

  /** Retrieves the `ix`th element, using an `Int` index. */
  final def apply(i: Int): A = apply(i.toLong)

  /** Returns a `Stepper` over the contents of this `AnyAccumulator`*/
  final def stepper: AnyStepper[A] = new AccumulatorStepper[A](this)

  /** Returns an `Iterator` over the contents of this `AnyAccumulator`. */
  final def iterator = stepper.iterator

  /** Returns a `java.util.Spliterator` over the contents of this `AnyAccumulator`*/
  final def spliterator: java.util.Spliterator[A] = stepper.spliterator

  /** Produces a sequential Java 8 Stream over the elements of this `AnyAccumulator`*/
  final def seqStream: java.util.stream.Stream[A] = java.util.stream.StreamSupport.stream(spliterator, false)

  /** Produces a parallel Java 8 Stream over the elements of this `AnyAccumulator`*/
  final def parStream: java.util.stream.Stream[A] = java.util.stream.StreamSupport.stream(spliterator, true)


  /** Copy the elements in this `AnyAccumulator` into an `Array` */
  override def toArray[B >: A : ClassTag]: Array[B] = {
    if (totalSize > Int.MaxValue) throw new IllegalArgumentException("Too many elements accumulated for an array: "+totalSize.toString)
    val a = new Array[B](totalSize.toInt)
    var j = 0
    var h = 0
    var pv = 0L
    while (h < hIndex) {
      val x = history(h)
      val n = cumulative(h) - pv
      pv = cumulative(h)
      var i = 0
      while (i < n) {
        a(j) = x(i).asInstanceOf[B]
        i += 1
        j += 1
      }
      h += 1
    }
    var i = 0
    while (i < index) {
      a(j) = current(i).asInstanceOf[B]
      i += 1
      j += 1
    }
    a
  }

  /** Copies the elements in this `AnyAccumulator` to a `List` */
  final override def toList: List[A] = {
    var ans: List[A] = Nil
    var i = index - 1
    while (i >= 0) {
      ans = current(i).asInstanceOf[A] :: ans
      i -= 1
    }
    var h = hIndex - 1
    while (h >= 0) {
      val a = history(h)
      i = (cumulative(h) - (if (h == 0) 0L else cumulative(h-1))).toInt - 1
      while (i >= 0) {
        ans = a(i).asInstanceOf[A] :: ans
        i -= 1
      }
      h -= 1
    }
    ans
  }


  /**
   * Copy the elements in this `AnyAccumulator` to a specified collection. Example use:
   * `acc.to(Vector)`.
   */
  override def to[C1](factory: Factory[A, C1]): C1 = {
    if (totalSize > Int.MaxValue) throw new IllegalArgumentException("Too many elements accumulated for a Scala collection: "+totalSize.toString)
    factory.fromSpecific(iterator)
  }
}

object AnyAccumulator extends collection.IterableFactory[AnyAccumulator] {
  private val emptyAnyRefArray = new Array[AnyRef](0)
  private val emptyAnyRefArrayArray = new Array[Array[AnyRef]](0)
  private val emptyLongArray = new Array[Long](0)

  /** A `Supplier` of `AnyAccumulator`s, suitable for use with `java.util.stream.Stream`'s `collect` method. */
  def supplier[A] = new java.util.function.Supplier[AnyAccumulator[A]]{ def get: AnyAccumulator[A] = new AnyAccumulator[A] }

  /** A `BiConsumer` that adds an element to an `AnyAccumulator`, suitable for use with `java.util.stream.Stream`'s `collect` method. */
  def adder[A] = new java.util.function.BiConsumer[AnyAccumulator[A], A]{ def accept(ac: AnyAccumulator[A], a: A): Unit = { ac += a } }

  /** A `BiConsumer` that adds an `Int` to an `AnyAccumulator`, suitable for use with `java.util.stream.Stream`'s `collect` method. */
  def unboxedIntAdder = new java.util.function.ObjIntConsumer[AnyAccumulator[Int]]{ def accept(ac: AnyAccumulator[Int], a: Int): Unit = { ac += a } }

  /** A `BiConsumer` that adds a `Long` to an `AnyAccumulator`, suitable for use with `java.util.stream.Stream`'s `collect` method. */
  def unboxedLongAdder = new java.util.function.ObjLongConsumer[AnyAccumulator[Long]]{ def accept(ac: AnyAccumulator[Long], a: Long): Unit = { ac += a } }

  /** A `BiConsumer` that adds a `Double` to an `AnyAccumulator`, suitable for use with `java.util.stream.Stream`'s `collect` method. */
  def unboxedDoubleAdder = new java.util.function.ObjDoubleConsumer[AnyAccumulator[Double]]{ def accept(ac: AnyAccumulator[Double], a: Double): Unit = { ac += a } }

  /** A `BiConsumer` that merges `AnyAccumulator`s, suitable for use with `java.util.stream.Stream`'s `collect` method. */
  def merger[A] = new java.util.function.BiConsumer[AnyAccumulator[A], AnyAccumulator[A]]{ def accept(a1: AnyAccumulator[A], a2: AnyAccumulator[A]): Unit = { a1 drain a2 } }

  def from[A](source: IterableOnce[A]): AnyAccumulator[A] = source match {
    case acc: AnyAccumulator[A] => acc
    case _ => new AnyAccumulator[A].addAll(source)
  }

  def empty[A]: AnyAccumulator[A] = new AnyAccumulator[A]

  def newBuilder[A]: mutable.Builder[A, AnyAccumulator[A]] = new AnyAccumulator[A]
}


trait AccumulatorFactoryInfo[A, C] {
  val companion: AnyRef
}
trait LowPriorityAccumulatorFactoryInfo {
  implicit def noAccumulatorFactoryInfo[A, C]: AccumulatorFactoryInfo[A, C] = noAccumulatorFactoryInfoPrototype.asInstanceOf[AccumulatorFactoryInfo[A, C]]
  private val noAccumulatorFactoryInfoPrototype: AccumulatorFactoryInfo[AnyRef, AnyRef] = new AccumulatorFactoryInfo[AnyRef, AnyRef] {
    val companion: AnyRef = null
  }
}
object AccumulatorFactoryInfo extends LowPriorityAccumulatorFactoryInfo {
  implicit def anyAccumulatorFactoryInfo[A]: AccumulatorFactoryInfo[A, AnyAccumulator[A]] = anyAccumulatorFactoryInfoPrototype.asInstanceOf[AccumulatorFactoryInfo[A, AnyAccumulator[A]]]

  private object anyAccumulatorFactoryInfoPrototype extends AccumulatorFactoryInfo[AnyRef, AnyAccumulator[AnyRef]] {
    val companion: AnyRef = AnyAccumulator
  }

  implicit val intAccumulatorFactoryInfo: AccumulatorFactoryInfo[Int, IntAccumulator] = new AccumulatorFactoryInfo[Int, IntAccumulator] {
    val companion: AnyRef = IntAccumulator
  }

  implicit val longAccumulatorFactoryInfo: AccumulatorFactoryInfo[Long, LongAccumulator] = new AccumulatorFactoryInfo[Long, LongAccumulator] {
    val companion: AnyRef = LongAccumulator
  }

  implicit val doubleAccumulatorFactoryInfo: AccumulatorFactoryInfo[Double, DoubleAccumulator] = new AccumulatorFactoryInfo[Double, DoubleAccumulator] {
    val companion: AnyRef = DoubleAccumulator
  }

  implicit val javaIntegerAccumulatorFactoryInfo: AccumulatorFactoryInfo[java.lang.Integer, IntAccumulator] = intAccumulatorFactoryInfo.asInstanceOf[AccumulatorFactoryInfo[java.lang.Integer, IntAccumulator]]
  implicit val javaLongAccumulatorFactoryInfo: AccumulatorFactoryInfo[java.lang.Long, IntAccumulator] = longAccumulatorFactoryInfo.asInstanceOf[AccumulatorFactoryInfo[java.lang.Long, IntAccumulator]]
  implicit val javaDoubleAccumulatorFactoryInfo: AccumulatorFactoryInfo[java.lang.Double, IntAccumulator] = doubleAccumulatorFactoryInfo.asInstanceOf[AccumulatorFactoryInfo[java.lang.Double, IntAccumulator]]
}
