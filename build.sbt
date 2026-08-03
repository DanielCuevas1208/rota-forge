ThisBuild / organization := "io.rotaforge"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.8"

lazy val root = (project in file("."))
  .settings(
    name := "rota-forge",
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all"),
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test,
    Compile / run / fork := true,
    Compile / run / connectInput := true,
    Test / parallelExecution := false
  )
