// Make sure that `magicImportBumpsNesting` does not accidentally cause spurious cycles (as was originally the case)
import Test.check

object Test {
  def check(desc: String, clazz: Class[_]) {
  }
}
