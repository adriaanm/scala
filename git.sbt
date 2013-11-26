import scalabuild._
import ScalaUtil._

gitHeadSha in ThisBuild := Process("git rev-parse HEAD").lines.head.take(10)

gitHeadDateTime in ThisBuild := {
  val Array(date, time, _) = Process("git log -1 --format=%ci HEAD").lines.head.split(" ", 3)
  date.replaceAll("-", "") +"-"+ time.replaceAll(":", "")
}
