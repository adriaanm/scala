object Util {
  def mono(x: Int) = x
  def poly[T](x: T): T = x
}

trait TFun { def map[T](f: Int => T): Unit = () }
object Fun extends TFun { import Util._
  def map[T: scala.reflect.ClassTag](f: Int => T): Unit = ()

// all ok:
//  map(mono)
//  map(mono _)
//  map(x => mono(x))

  map(poly) // todo
  map(poly _) // todo
//  map(x => poly(x)) // ok
}

trait SAM[-T, +R] { def apply(x: T): R }

trait TSam { def map[T](f: Int `SAM` T): Unit = () }
object Sam extends TSam { import Util._
  def map[T: scala.reflect.ClassTag](f: Int `SAM` T): Unit = ()

  map(mono) // todo
  map(mono _) // todo
//  map(x => mono(x)) // ok

  map(poly) // todo
  map(poly _) // todo
//  map(x => poly(x)) // ok
}
