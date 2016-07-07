trait WebDriver 

trait WebBrowser {
  def captureTo(fileName: String)(implicit driver: WebDriver): Unit = ???

  implicit val webDriver: WebDriver
}


// these classes must have overloaded constructors
class FirefoxProfile() {  def this(profileDir: String) = { this() } }
class FirefoxDriver(profile: FirefoxProfile) extends WebDriver {  def this(desiredCapabilities: String) = { this(null: FirefoxProfile) } }

trait Firefox extends WebBrowser {
  // missing result type
  // note: replacing val with def causes this to break on 2.12.0-M5 and older as well
  val firefoxProfile = new FirefoxProfile()

  // XXX cycle while inferring result type for this val (when it comes after firefoxProfile)
  // note: replacing val with def causes this to break on 2.12.0-M5 and older as well
  implicit val webDriver = new FirefoxDriver(firefoxProfile)

  def captureScreenshot(directory: String): Unit = captureTo(directory)
}
