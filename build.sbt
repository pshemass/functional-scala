import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion     := "2.12.9"
ThisBuild / version          := "0.1.6"
ThisBuild / organization     := "zio"
ThisBuild / organizationName := "zio"


lazy val root = (project in file("."))
  .settings(
    name := "zio-keeper-examples",
    libraryDependencies += "dev.zio" %% "zio-keeper" % "0.0.0+91-5fc52656", // publishLocal on zio-keeper master
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
  )

scalacOptions ++= Seq("-Ypartial-unification", "-Ywarn-value-discard")

