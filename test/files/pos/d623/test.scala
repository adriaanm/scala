import jkson.Generic

abstract class ScalaGen[T] extends Generic[T]

abstract class ScalaMono extends Generic[Product] {
  // Crazy workaround for https://github.com/scala/scala-dev/issues/623
  override def foo(): Product with Object = super.foo().asInstanceOf[Product with Object]
}
