import sbt._
import sbt.Keys._
import play.Play.autoImport._
import play._

import scala.language.postfixOps

object OctopartsBuild extends Build {

  import Common._

  val octopartsVersion = "2.3-SNAPSHOT"

  val httpPort = 9000
  val theScalaVersion = "2.11.2"
  val thePlayVersion = "2.3.6" // make play-json-formats subproject depend on play-json when bumping to 2.4
  val slf4jVersion = "1.7.7"
  val hystrixVersion = "1.3.18"
  val httpClientVersion = "4.3.5"
  val scalikejdbcVersion = "2.1.2"
  val swaggerVersion = "1.3.10"
  val jacksonVersion = "2.4.3"

  val testEnv = sys.env.get("PLAY_ENV") match {
    case Some("ci") => "ci"
    case _ => "unit_testing"
  }

  // -------------------------------------------------------
  // Interface for authentication plugins
  // -------------------------------------------------------
  lazy val authPluginApi = Project(id = "auth-plugin-api", base = file("plugins/auth-plugin-api"), settings = nonPlayAppSettings).settings(
    name := "octoparts-auth-plugin-api",
    libraryDependencies ++= Dependencies.authPluginDependencies
  )

  // -------------------------------------------------------
  // Model classes
  // -------------------------------------------------------
  lazy val models = Project(id = "models", base = file("models"), settings = nonPlayAppSettings)
    .settings(
      name := "octoparts-models",
      libraryDependencies ++= Dependencies.modelsDependencies
    )


  // -------------------------------------------------------
  // Java client
  // -------------------------------------------------------
  lazy val javaClient = {
    Project(id = "java-client", base = file("java-client"), settings = nonPlayAppSettings)
      .settings(
        name := "octoparts-java-client",
        javacOptions in compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint"),
        javacOptions in doc ++= Seq("-source", "1.6"),
        libraryDependencies ++= Dependencies.javaClientDependncies
      )
      .dependsOn(models)
  }

  // -------------------------------------------------------
  // Play-JSON-formats
  // -------------------------------------------------------
  lazy val playJsonFormats = Project(id = "play-json-formats", base = file("play-json-formats"), settings = nonPlayAppSettings)
    .settings(
      name := "octoparts-play-json-formats",
      libraryDependencies ++= Seq(
        ws, //TODO when upgrading to Play 2.4; change this to use just play-json
        "org.scalatest" %% "scalatest" % "2.2.2" % "test",
        "org.scalatestplus" %% "play" % "1.2.0" % "test"
      )
    )
    .dependsOn(models)

  // -------------------------------------------------------
  // Scala-WS-client
  // -------------------------------------------------------
  lazy val scalaWsClient = Project(id = "scala-ws-client", base = file("scala-ws-client"), settings = nonPlayAppSettings)
    .settings(
      name := "octoparts-scala-ws-client",
      libraryDependencies ++= Seq(
        ws,
        "org.scalatest" %% "scalatest" % "2.2.2" % "test",
        "org.scalatestplus" %% "play" % "1.2.0" % "test"
      )
    )
    .dependsOn(models, playJsonFormats)

  // -------------------------------------------------------
  // Play app
  // -------------------------------------------------------
  lazy val app = Project(id = "octoparts", base = file("."), settings = playAppSettings)
    .enablePlugins(PlayScala)
    .dependsOn(models, authPluginApi, playJsonFormats)
    .aggregate(scalaWsClient, javaClient, models, authPluginApi, playJsonFormats)

}