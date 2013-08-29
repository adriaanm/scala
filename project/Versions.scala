package scalabuild

import sbt._

object Versions {
  private def v(s: String) = Version(s)
  // Versions of artifacts upon which we depend
  object Deps {
    def starr   = v("2.11.0-M4")
    def parsers = v("1.0-RC1")
    def xml     = v("1.0-RC2")
    def partest = v("1.0-RC4")
    def ant     = v("1.9.2")
    def jline   = v("2.11")
  }
  // Versions of artifacts which we are presently building
  object Build {
    private def suffix = "SNAPSHOT"
    def core    = v(s"2.11.0-$suffix")
    def parsers = v(s"1.1-$suffix")
    def xml     = v(s"1.1-$suffix")
  }
}
object Deps {
  import Versions.Deps.starr
  def ant      = "org.apache.ant" % "ant" % Versions.Deps.ant
  def jline    = "jline" % "jline" % Versions.Deps.jline
  def partest  = "org.scala-lang.modules" % s"scala-partest_$starr" % Versions.Deps.partest
  def library  = "org.scala-lang" % "scala-library" % starr
  def compiler = "org.scala-lang" % "scala-compiler" % starr % "scala-tool"
}

final case class Version(version: String) { override def toString = version }
object Version { implicit def lowerVersion(v: Version): String = v.version }
