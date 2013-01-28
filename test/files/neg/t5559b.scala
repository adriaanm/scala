// should compile, but should definitely not crash
// for now all we're testing is not-crashing
object Bug {
  trait H[F[_]]
  def f[F[_], T, FT <: F[T]](h : H[F] ) = 1
  f(new H[Set]{})
}