import Dependencies._
import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion     := "2.12.9"
ThisBuild / version          := "0.1.6"
ThisBuild / organization     := "zio"
ThisBuild / organizationName := "zio"


lazy val root = (project in file("."))
  .settings(
    name := "zio-keeper-examples",
    dockerExposedPorts := Seq(5558, 9090),
    dockerRepository := Some("rzbikson"),
    libraryDependencies += "dev.zio" %% "zio-keeper" % "0.0.0+91-5fc52656",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "io.prometheus" % "simpleclient" % "0.6.0",
    libraryDependencies += "io.prometheus" % "simpleclient_hotspot" % "0.6.0",
    libraryDependencies +="io.prometheus" % "simpleclient_httpserver" % "0.6.0",
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

scalacOptions ++= Seq("-Ypartial-unification", "-Ywarn-value-discard")

