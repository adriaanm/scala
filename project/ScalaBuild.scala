package scalabuild

import sbt._
import Keys._

object BuildLogic extends Build {
  def buildVersion = Versions.Build.core
  def localMaven   = Resolver.file("file", file(Path.userHome.absolutePath+"/.m2/repository"))

  implicit class ProjectOps(val p: Project) extends AnyVal {
    def core: Project             = createCore(p, p.id)
    def core(id: String): Project = createCore(p, id)
    def module: Project           = createModule(p, p.id, buildVersion)
    def module(id: String = p.id, v: Version = buildVersion): Project = createModule(p, id, v)

    def also(id: ModuleID): Project = p settings (libraryDependencies += id)
  }
  def rootSettings = List(
                           name := "scala",
                        version := buildVersion,
     publishArtifact in Compile := false,
                      mainClass := Some("scala.tools.nsc.MainGenericRunner")
  )
  def publishSettings = List(
                                   publishTo := Some(localMaven),
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false
  )
  def scalaSettings = List(
               scalaHome := Some(baseDirectory in ThisBuild value),
            scalaVersion := Versions.Deps.starr,
      scalaBinaryVersion := buildVersion,
    managedScalaInstance := false,
        autoScalaLibrary := false,
           unmanagedBase := file("lib_unmanaged")
  )
  def commonSettings = publishSettings ++ scalaSettings ++ List(
       scalacOptions := "-nowarn" :: Nil,
        javacOptions := "-nowarn" :: Nil,
       sourcesInBase := false
  )

  private def createCore(project: Project, id: String): Project =
    create(project, id) settings (
      organization := "org.scala-lang",
           version := buildVersion,
      crossVersion := CrossVersion.Disabled
    )

  private def createModule(project: Project, id: String, rev: Version): Project =
    create(project, id) settings (
              name := s"scala-$id",
      organization := "org.scala-lang.modules",
           version := rev,
      crossVersion := CrossVersion.fullMapped(_ => buildVersion)
    )

  private def create(project: Project, id: String): Project = {
    def settings = commonSettings ::: List(
      name := s"scala-$id",
      scalaSource in Compile := (baseDirectory in ThisBuild).value / "src" / id
    )
    project in (file("projects") / id) settings (settings: _*)
  }
}
