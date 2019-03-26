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

package scala
package collection
package mutable

import scala.annotation.unchecked.uncheckedVariance
import scala.language.higherKinds

/**
  * Base type for mutable sorted set collections
  */
trait SortedSet[A]
  extends Set[A]
    with collection.SortedSet[A]
    with SortedSetOps[A, SortedSet, SortedSet[A]] {

  override def unsorted: Set[A] = this

  override def sortedIterableFactory: SortedIterableFactory[SortedSet] = SortedSet

  override protected def fromSpecific(coll: IterableOnce[A] @uncheckedVariance): SortedSet[A] = sortedIterableFactory.from(coll)
  override protected def newSpecificBuilder: mutable.Builder[A, SortedSet[A]] = sortedIterableFactory.newBuilder[A]
  override def empty: SortedSet[A] = sortedIterableFactory.empty

  override def withFilter(p: A => Boolean): SortedSetOps.WithFilter[A, Set, mutable.SortedSet] = new SortedSetOps.WithFilter(this, p)
}

/**
  * @define coll mutable sorted set
  * @define Coll `mutable.Sortedset`
  */
trait SortedSetOps[A, +CC[X] <: SortedSet[X], +C <: SortedSetOps[A, CC, C]]
  extends SetOps[A, Set, C]
    with collection.SortedSetOps[A, CC, C] {

  def unsorted: Set[A]

  override def withFilter(p: A => Boolean): SortedSetOps.WithFilter[A, Set, CC] = new SortedSetOps.WithFilter(this, p)
}

/**
  * $factoryInfo
  * @define coll mutable sorted set
  * @define Coll `mutable.Sortedset`
  */
@SerialVersionUID(3L)
object SortedSet extends SortedIterableFactory.Delegate[SortedSet](TreeSet)
