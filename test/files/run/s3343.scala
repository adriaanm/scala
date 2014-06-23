object Test extends App {
  def tryPrint(s: String, cls: Class[_]) {
    println(s+" class - "+cls)
    println(s+" encl - "+cls.getEnclosingClass)
    try println(s + " simple - " + cls.getSimpleName)
    catch {
      case t: Throwable =>
      println(s + " - unable to get simple name for " + cls)
      t.printStackTrace(System.out)
    }
  }

  tryPrint("Test ", Test.getClass)
  tryPrint("i1 ", i1.getClass)
  tryPrint("i1_1 ", classOf[i1.i1_1])
  tryPrint("i1_2 ", i1.i1_2.getClass)

  object i1 {
    class i1_1
    object i1_2
  }

}
