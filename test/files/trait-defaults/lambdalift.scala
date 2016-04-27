// WIP test case -- need to check that the MouseHandler constructor does not receive any arguments
// the capture in def mouseClicked should be restricted to that method
class Lift {
  def foo = {
    // this will be captured by the MouseHandler trait,
    // which gives rise to a new trait field during LambdaLift
    var Clicked = "Clicked"

    def bar = Clicked
    
    trait MouseHandler {
      // the method should be considered capturing Clicked, not the trait
      // so, it'll receive an extra argument
      def mouseClicked = Clicked + bar
    }
    
    class CC extends MouseHandler
    
    // new C {}
    (new CC).mouseClicked
  }
}

// TODO: this crashes the compiler
object O extends Lift with App {
  println(foo)
}
