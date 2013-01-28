class Test {
  def f[T](x1: Set[T]) = () => new { def apply(x2: Set[_ <: T]) = List(x1, x2) }
}