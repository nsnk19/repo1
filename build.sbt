name := "starts_per_second_1"

version := "0.1"

scalaVersion := "2.12.4"

val jacksonVersion = "2.8.4"

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "2.3.3",
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
)