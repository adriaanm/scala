package scalabuild

import sbt._

object Versions {
  // Versions of artifacts upon which we depend
  object Deps {
    // compiler used to build core
    val parsersVer    = settingKey[String]("scala-parser-combinators.version.number")
    val xmlVer        = settingKey[String]("scala-xml.version.number")
    val antVer        = settingKey[String]("ant")
    val jlineVer      = settingKey[String]("jline.version.number")
    val partestVer    = settingKey[String]("partest.version.number")
    val partestSbtVer = settingKey[String]("partest-interface.version.number")
    val scalacheckVer = settingKey[String]("scalacheck.version.number")
  }

  // Versions of artifacts which we are presently building
  object Build {
    val suffix      = settingKey[String]("version suffix")
    val osgiVersion = settingKey[String]("osgi")
  }
}
