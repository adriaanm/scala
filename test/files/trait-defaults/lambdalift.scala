class Lift {
  def foo = {
    // this will be captured by the MouseHandler trait,
    // which gives rise to a new trait field during LambdaLift
    var Clicked = "Clicked"

    def bar = Clicked
    
    trait MouseHandler {
      def mouseClicked = {println(Clicked) ; println(bar)}
    }
    
    trait C extends MouseHandler
    class CC extends C
    
    new C {}
    new CC
  }
}

object O extends Lift with App {
  println(foo)
}
