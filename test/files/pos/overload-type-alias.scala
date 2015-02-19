trait Builder[El] {
  type Res
  def +=(x: El): this.type
  def result(): Res
}

trait Trav[T] {
  // new:
  implicit type BuilderFor[U] <: Builder[U]
  // desugars to a synthetic method like this, the only way to get an implicit value of type BuilderFor[_]
  implicit def BuilderFor[T]: BuilderFor[T] = ??? // = new BuilderFor[T] <-- must consider exact type of this
  
  def foreach(f: T => Unit): Unit

  // TOOD: syntactic sugar for implicit types, so you can write BuilderFor[U]#Res (which desugars to what we have now)
  def map[U](f: T => U)(implicit b: BuilderFor[U]): b.Res = {
    foreach { x => b += f(x) }
    b.result()
  }
}

class Set[T] extends Trav[T] {
  def foreach(f: T => Unit): Unit = ???
}

class SetBuilder[El] extends Builder[El] {
  type Res = Set[El]
  def +=(x: El): this.type = { println(s"roger! Set-building with $x"); this }
  def result: Res = new Set[El]
}

class BitSetBuilder extends Builder[Int] {
  type Res = BitSet
  private[this] var theBit: Int = 0
  def +=(x: Int): this.type = { println(s"roger! BitSet-building with $x"); theBit = x; this }
  def result: Res = new BitSet(theBit)
}


class BitSet(val bit: Int) extends Set[Int] {
  // new:
  implicit type BuilderFor[U] = SetBuilder[U]
  implicit type BuilderFor[U <: Int] = BitSetBuilder
  val x: BitSetBuilder = implicitly[BuilderFor[Int]]
  
  override def foreach(f: Int => Unit): Unit = f(bit)
}