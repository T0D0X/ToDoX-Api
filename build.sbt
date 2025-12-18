ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

val zioVersion = "2.1.22"
val doobieVersion = "1.0.0-RC11"
val testContainersVersion = "0.43.6"
val tapirVersion = "1.13.2"

libraryDependencies ++= Seq(
		"org.postgresql" % "postgresql" % "42.7.8",

		"dev.zio" %% "zio" % zioVersion,
		"dev.zio" %% "zio-streams" % zioVersion,
		"dev.zio" %% "zio-http" % "3.7.0",

		"dev.zio" %% "zio-json" % "0.7.45",
		"dev.zio" %% "zio-interop-cats" % "23.1.0.5",

		"org.tpolecat" %% "doobie-postgres" % doobieVersion,

		"com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
		"com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion,

		"com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.12.5",
		"org.http4s" %% "http4s-ember-server" % "0.23.33",
		"org.flywaydb" % "flyway-core" % "11.19.0",
		"org.flywaydb" % "flyway-database-postgresql" % "11.19.0",

		"org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
		"dev.zio" %% "zio-test" % zioVersion,
		"dev.zio" %% "zio-test-sbt" % zioVersion % Test,
		"dev.zio" %% "zio-mock" % "1.0.0-RC12" % Test,
		"org.scalamock" %% "scalamock" % "7.5.2" % Test,
		"com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
		"org.testcontainers" % "postgresql" % "1.21.3" % Test,
		"org.slf4j" % "slf4j-simple" % "2.0.17" % Test
)

lazy val root = (project in file("."))
.settings(
		name := "ToDoX-Api"
)

enablePlugins(JavaAppPackaging, DockerPlugin)

// Базовые настройки Docker
Docker / packageName := "todox-api"
Docker / version := "1.0"
dockerBaseImage := "eclipse-temurin:21-jdk"
dockerExposedPorts := Seq(8080)


Compile / mainClass := Some("TodoApp")

bashScriptExtraDefines := Seq()


testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
