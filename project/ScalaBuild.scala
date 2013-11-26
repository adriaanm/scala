package scalabuild

import sbt._
import Keys._

object Settings {
  import ScalaUtil._
  import Versions.Deps._
  import Versions.Build._

  def publishSettings = List(      publishTo := Some(Resolver.mavenLocal),
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false
  )

  // used to build core -- we only use released STARRs, and for staged builds we first publish locker and reboot
  def scalaSettings = Seq(
    conflictWarning ~= { _.copy(failOnConflict = false) }, // TODO: can we avoid this warning altogether?
      scalaBinaryVersion := prop("scala.binary.version"),
            scalaVersion := prop("starr.version"),
           unmanagedBase := file("lib_unmanaged") // hiding ~/lib from being found for unmanaged classpath
    // managedScalaInstance := false,
    // autoScalaLibrary := false,
  )
  def commonSettings = publishSettings ++ scalaSettings ++ versionSettings ++ Seq(
    organization := "org.scala-lang",
    scalacOptions in Compile += "-nowarn",
     javacOptions in Compile += "-nowarn"
  )

  def versionSettings = Seq(
    suffix         := prop("version.suffix", prop("maven.version.suffix", "-SNAPSHOT")),
    version        := s"${major}.${minor}.${patch}${buildSuffix}${suffix.value}",
    osgiVersion    := s"${major}.${minor}.${patch}.v${gitHeadDateTime.value}${osgify(suffix.value)}-${gitHeadSha.value}",

    antVer         := "1.9.2",
    jlineVer       := prop("jline.version.number"),
    parsersVer     := prop("scala-parser-combinators.version.number"),
    xmlVer         := prop("scala-xml.version.number"),
    partestVer     := prop("partest.version.number"),
    partestSbtVer  := prop("partest-interface.version.number"),
    scalacheckVer  := prop("scalacheck.version.number")
  )

  private lazy val major       = prop("version.major")
  private lazy val minor       = prop("version.minor")
  private lazy val patch       = prop("version.patch")
  private lazy val buildSuffix = prop("version.bnum") match { case "0" => "" case b => s"-$b" }
  private def osgify(sfx: String) = sfx match {
    case ""           => "-VFINAL"
    case "-SNAPSHOT"  => ""
    case sfx          => sfx
  }
}

object ScalaUtil {
  import java.io.{InputStreamReader, FileInputStream, File}
  import java.util.Properties

  val gitHeadSha = settingKey[String]("git commit hash of HEAD")
  val gitHeadDateTime = settingKey[String]("git commit hash of HEAD")

  def prop(n: String)  = sys props n
  def prop(n: String, d: => String) = sys.props.getOrElse(n, d)

  def loadProps(file: File): Unit = {
    import scala.collection.JavaConverters._
    if (file.exists()) {
      println("Loading system properties from file `" + file.name + "`")
      val in = new InputStreamReader(new FileInputStream(file), "UTF-8")
      val props = new Properties
      props.load(in)
      in.close()
      sys.props ++ props.asScala
    }
  }
}
