name := "elasticsearch-generate-data"

scalaVersion               := "2.11.6"
version := "0.1"

val sprayVersion = "1.3.3"
val sprayHttp = "io.spray" %% "spray-http" % sprayVersion
val sprayClient = "io.spray" %% "spray-client" % sprayVersion

val akkaVersion = "2.3.13"
val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

val jCommander = "com.beust" % "jcommander" % "1.48"
val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"

libraryDependencies += sprayHttp
libraryDependencies += sprayClient
libraryDependencies += akkaActor
libraryDependencies += jCommander
libraryDependencies += scalaParser