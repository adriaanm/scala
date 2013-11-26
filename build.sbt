import scalabuild._, Settings._, ScalaUtil._, Versions.Deps._

// TODO: this isn't always triggered... insert this into dependency graph the right way
initialize := {
  loadProps((baseDirectory in ThisBuild value) / "versions.properties")
  loadProps((baseDirectory in ThisBuild value) / "build.number")
}

// Don't know how to get a root project which aggregates all projects except
// by not defining any root project. Then how do I refer to the not-defined project?
lazy val scala = project in file(".") settings (commonSettings: _*) settings (
                        name := "scala",
                   mainClass := Some("scala.tools.nsc.MainGenericRunner"),
  publishArtifact in Compile := false) aggregate (allRefs: _*)


def core(project: Project) = project in (file("projects") / project.id) settings ((
  commonSettings ++ Seq(
    name := s"scala-${project.id}",
    scalaSource in Compile := (baseDirectory in ThisBuild).value / "src" / project.id)) : _*)

def java(project: Project) = project in (file("projects") / project.id) settings ((
  commonSettings ++ Seq(
    name := s"${project.id}",
    javacOptions ++= Seq("-source", "1.5", "-target", "1.6"),
    javaSource in Compile := (baseDirectory in ThisBuild).value / "src" / project.id)) : _*)

lazy val asm = java(project)

lazy val forkjoin = java(project) settings (
  javacOptions += "-XDignore.symbol.file"
)

// core
lazy val library = core(project) settings (
  scalacOptions in Compile ++= "-sourcepath" :: (scalaSource in Compile).value.toString :: Nil
) dependsOn forkjoin

lazy val reflect = core(project) dependsOn library

lazy val compiler = core(project) dependsOn (asm, reflect) settings (
  libraryDependencies += "org.apache.ant" % "ant" % antVer.value)

// library modules
lazy val swing  = core(project) dependsOn library

lazy val actors = core(project) dependsOn (library, forkjoin)

lazy val scalap = core(project) dependsOn compiler

// compiler modules
lazy val interactive = core(project) dependsOn (compiler, scaladoc)

lazy val repl = core(project) dependsOn compiler settings (
  libraryDependencies += "jline" % "jline" % jlineVer.value)

lazy val scaladoc = core(project) dependsOn (compiler, partestExtras)

// testing infrastructure
lazy val partestExtras = core(project) settings (
  name := "partest-extras",
  libraryDependencies += "org.scala-lang.modules" %% "scala-partest" % partestVer.value)



def autonomousProjects = List(asm, forkjoin)

def coreProjects = List(library, reflect, compiler)

def moduleProjects = List(actors, swing, interactive, repl, scaladoc, scalap, partestExtras)

def allProjects = autonomousProjects ++ coreProjects ++ moduleProjects

def allRefs = allProjects map Project.projectToRef
