name := """akka-sample-main-scala"""

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "io.spray" % "spray-can" % "1.3.1",
  "io.spray" % "spray-client" % "1.3.1",
  "com.typesafe.akka" % "akka-testkit_2.10" % "2.3.2",
  "org.scalatest" % "scalatest_2.10" % "2.1.3" % "test",
  "com.typesafe.akka" % "akka-persistence-experimental_2.10" % "2.3.2",
  "com.typesafe.akka" % "akka-cluster_2.10" % "2.3.2"
)

