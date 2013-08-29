import scalabuild._, BuildLogic._

// Populate the repo with bootstrap jars
initialize := s"${baseDirectory.value}/pull-binary-libs.sh".!

// Don't know how to get a root project which aggregates all projects except
// by not defining any root project. Then how do I refer to the not-defined project?
lazy val scala = project.asRoot aggregate (allRefs: _*)

lazy val library = project.core settings (
  scalacOptions in Compile ++= "-sourcepath" :: (scalaSource in Compile).value.toString :: Nil
) dependsOn forkjoin

lazy val asm, forkjoin = project.core

lazy val reflect, swing = project.core dependsOn library

lazy val compiler = project.core dependsOn (asm, reflect) also Deps.ant

lazy val repl = project.core dependsOn compiler also Deps.jline

lazy val actors = project.core dependsOn (library, forkjoin)

lazy val scalap = project.core dependsOn compiler

lazy val scaladoc = project.core dependsOn (compiler, parserCombinators, partestExtras)

lazy val interactive = project.core dependsOn (compiler, scaladoc)

lazy val xml = project.module(v = Versions.Build.xml) dependsOn library

lazy val parserCombinators = project.module(id = "parser-combinators", v = Versions.Build.parsers) dependsOn library

lazy val partestExtras = project.module(id = "partest-extras") also Deps.partest

def autonomousProjects = List(asm, forkjoin)

def coreProjects = List(library, reflect, compiler, repl, swing, actors, scaladoc, interactive, scalap)

def moduleProjects = List(xml, parserCombinators, partestExtras)

def allProjects = autonomousProjects ++ coreProjects ++ moduleProjects

def allRefs = allProjects map Project.projectToRef
