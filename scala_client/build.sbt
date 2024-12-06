scalaVersion := "2.13.12"

name := "scala-client"
organization := "ch.epfl.scala"
version := "1.0"

libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.9.2"
libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.9.2"
