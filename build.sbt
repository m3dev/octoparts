import Common._

/**
 * Helper for defining a new non-Play sub-project
 *
 * @param projectName Name of the project
 * @param baseFile Where the base of the project is, defaults to a directory from root named the same as projectName
 */
def nonPlayProject(projectName: String)(baseFile: String = projectName): Project =
  Project(id = projectName, base = file(baseFile), settings = nonPlayAppSettings).settings(name := projectName)

// -------------------------------------------------------
// Play app
// -------------------------------------------------------
lazy val app = Project(id = "octoparts", base = file("."), settings = playAppSettings)
  .enablePlugins(PlayScala)
  .dependsOn(models, authHandlerApi, playJsonFormats, scalaWsClient % "test->compile")
  /*
   * The "test->compile" above adds a test-mode dependency in the current project definition (root app in this
   * case); in particular the ScalaWsClient project is to be in a compiled state before running tests in root.
   *
   * The reason I added this was to allow us to integration test the root project (Octoparts app)
   * using the ScalaWsClient.
   *
   * For more info see: http://www.scala-sbt.org/0.13/docs/Multi-Project.html#Per-configuration+classpath+dependencies
  */
  .aggregate(scalaWsClient, javaClient, models, authHandlerApi, playJsonFormats)

// -------------------------------------------------------
// Interface for authentication handling
// -------------------------------------------------------
lazy val authHandlerApi = nonPlayProject("auth-handler-api")("auth-handler-api")
  .settings(
    name := "octoparts-auth-handler-api",
    libraryDependencies ++= Dependencies.authHandlerDependencies
  )

// -------------------------------------------------------
// Model classes
// -------------------------------------------------------
lazy val models = nonPlayProject("models")()
  .settings(
    name := "octoparts-models",
    libraryDependencies ++= Dependencies.modelsDependencies
  )

// -------------------------------------------------------
// Java client
// -------------------------------------------------------
lazy val javaClient = nonPlayProject("java-client")()
  .settings(
    name := "octoparts-java-client",
    libraryDependencies ++= Dependencies.javaClientDependncies
  )
  .dependsOn(models)

// -------------------------------------------------------
// Play-JSON-formats
// -------------------------------------------------------
lazy val playJsonFormats = nonPlayProject("play-json-formats")()
  .settings(
    name := "octoparts-play-json-formats",
    libraryDependencies ++= Dependencies.playJsonFormatsDependencies
  )
  .dependsOn(models)

// -------------------------------------------------------
// Scala-WS-client
// -------------------------------------------------------
lazy val scalaWsClient =  nonPlayProject("scala-ws-client")()
  .settings(
    name := "octoparts-scala-ws-client",
    libraryDependencies ++= Dependencies.scalaWsClientDependencies
  )
  .dependsOn(models, playJsonFormats)
