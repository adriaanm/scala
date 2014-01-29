class Test {
  class MyExist extends ExistF[MyExist]
  // SI-8197, SI-6169: java infers the bounds of existentials, so we have to as well...
  def stringy: Exist[_ <: String] = (new Exist[String]).foo
  def fbounded: (ExistF[t] forSome {type t <: ExistF[t] }) = (new MyExist).foo
}