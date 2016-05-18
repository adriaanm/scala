//import Settings._

(incOptions in ThisBuild) := (incOptions in ThisBuild).value.withNameHashing(false).withAntStyle(true)

//embedJLine in Global := false

publishArtifact in (Compile, packageDoc) in ThisBuild := false
