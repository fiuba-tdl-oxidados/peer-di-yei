import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val peerDiYei = (project in file("."))
  .settings(
    name := "peer-di-yei"
  )

scalaVersion := "2.13.13"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val akkaVersion = sys.props.getOrElse("akka.version", "2.9.3")
lazy val akkaHttpVersion = sys.props.getOrElse("akka.http.version", "10.6.3")

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "com.typesafe.play" %% "play-json" % "2.10.5",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.github.scopt" %% "scopt" % "4.0.1"
)
