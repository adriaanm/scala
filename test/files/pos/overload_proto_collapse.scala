
class Test {
  def prepended[B >: Char](elem: B): String = ???
  def prepended(c: Char): String = ???

  def +:[B >: Char](elem: B): String = prepended(elem)
}


trait DurationConversions {
  trait Classifier[C] { type R }

  def days: Int = ???
  def days[C](c: C)(implicit ev: Classifier[C]): ev.R = ???

  def day[C](c: C)(implicit ev: Classifier[C]): ev.R = days(c)
}


trait AnonMatch {
  trait MapOps[K, +V, +CC[_, _]] {
    def map[K2, V2](f: ((K, V)) => (K2, V2)): CC[K2, V2] = ???
    def map[K2 <: AnyRef, V2](f: ((K with AnyRef, V)) => (K2, V2)): MapOps[K2, V2, Map] = ???
  }

  (??? : MapOps[String, Int, Map]).map{ case (k,v) => ??? }
}


trait FBounds {
  def f[A](x: A) = 11;
  def f[A <: Ordered[A]](x: Ordered[A]) = 12;

  f(1)
}
