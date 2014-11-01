import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.SbtScalariform._
import net.virtualvoid.sbt.graph.Plugin._
import sbt._
import sbt.Keys._
import sbtbuildinfo.Plugin._
import scoverage.ScoverageSbtPlugin
import scoverage.ScoverageSbtPlugin._
import org.scoverage.coveralls.CoverallsPlugin.coverallsSettings
import xerial.sbt.Sonatype._
import SonatypeKeys._
import com.typesafe.sbt.SbtNativePackager.NativePackagerKeys._

import play.PlayImport.PlayKeys._
import play.Play.autoImport._
import play._

import scala.language.postfixOps
import scalariform.formatter.preferences._

object OctopartsBuild extends Build {

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

  /*
   * Settings that are common to every single project
   */
  lazy val commonSettings = Seq(
    organization := "com.m3",
    version := octopartsVersion,
    scalaVersion := theScalaVersion
  ) ++
    graphSettings ++
    scalariformSettings ++
    formatterPrefs ++
    compilerSettings ++
    resolverSettings ++
    ideSettings ++
    testSettings ++
    Scoverage.settings

  /*
   * Settings that are common for every project _except_ the Play app
   * (because we don't want to publish the Play app to Maven Central)
   */
  lazy val nonPlayAppSettings =
    commonSettings ++
    Publishing.settings

  /*
   * Settings for the Play app
   */
  lazy val playAppSettings =
      commonSettings ++
      playSettings ++
      BuildInfo.settings ++
      addConfDirToClasspathSettings ++
      excludeConfFilesFromJarSettings ++
      sonatypeSettings ++
      coverallsSettings ++
      Seq(
        publishArtifact := false,
        libraryDependencies ++= Dependencies.rootDependencies
      )

  lazy val playSettings = Seq(
    playVersion := thePlayVersion,
    playDefaultPort := httpPort
  )

  lazy val addConfDirToClasspathSettings = Seq(
    // Add the contents of the /conf dir of an unzipped tarball to the start of the classpath
    // so we can edit the config files of a deployed app.
    // See https://github.com/playframework/playframework/issues/3473
    scriptClasspath := "../conf" +: scriptClasspath.value
  )


  lazy val excludeConfFilesFromJarSettings = Seq(
    // Remove config files from the jar, because they conflict with the ones in the /conf directory.
    // (e.g. Play finds both play.plugins files and loads all the plugins twice...)
    // Note: a mapping is a (java.io.File, String) tuple
    mappings in (Compile, packageBin) ~= { m =>
      m.filterNot { case (from: java.io.File, _) =>
        from.getName.endsWith(".conf") ||
        from.getName.endsWith(".xml") ||
        from.getName.endsWith(".plugins")
      }
    }
  )

  lazy val resolverSettings = {
    // Use in-house Maven repo instead of Maven central if env var is set
    sys.env.get("INHOUSE_MAVEN_REPO").fold[Seq[sbt.Def.Setting[_]]] {
      Seq(
        resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
        resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      )
    } { inhouse =>
      Seq(
        // Speed up resolution using local Maven cache
        resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
        resolvers += "Inhouse" at inhouse,
        externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)
      )
    }
  }

  lazy val ideSettings = Seq(
    // Faster "sbt gen-idea"
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)
  )

  lazy val compilerSettings = Seq(
    // the name-hashing algorithm for the incremental compiler.
    incOptions := incOptions.value.withNameHashing(nameHashing = true),

    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint")
  )

  lazy val testSettings = Seq(Test, ScoverageSbtPlugin.ScoverageTest).flatMap { t =>
    Seq(parallelExecution in t := false) // Avoid DB-related tests stomping on each other
  }

  lazy val formatterPrefs = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
  )

  // -------------------------------------------------------
  // Interface for authentication plugins
  // -------------------------------------------------------
  lazy val authPluginApi = Project(id = "auth-plugin-api", base = file("plugins/auth-plugin-api"), settings = nonPlayAppSettings).settings(
    name := "octoparts-auth-plugin-api",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % thePlayVersion % "provided",
      "com.beachape" %% "ltsv-logger" % "0.0.8"
    )
  )

  // -------------------------------------------------------
  // Model classes
  // -------------------------------------------------------
  lazy val models = Project(id = "models", base = file("models"), settings = nonPlayAppSettings)
    .settings(
      name := "octoparts-models",
      libraryDependencies ++= Seq(
        "com.wordnik" % "swagger-annotations" % swaggerVersion intransitive(),
        "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion intransitive(),
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion intransitive()
      ),
      crossScalaVersions := Seq("2.10.4", theScalaVersion),
      crossVersion := CrossVersion.binary
    )


  // -------------------------------------------------------
  // Java client
  // -------------------------------------------------------
  lazy val javaClient = {
    Project(id = "java-client", base = file("java-client"), settings = nonPlayAppSettings)
      .settings(
        name := "octoparts-java-client",
        crossScalaVersions := Seq("2.10.4", theScalaVersion),
        javacOptions in compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint"),
        javacOptions in doc ++= Seq("-source", "1.6"),

        libraryDependencies ++= Seq(
          "com.google.code.findbugs" % "jsr305" % "3.0.0" intransitive(),
          "org.slf4j" % "slf4j-api" % slf4jVersion,
          "com.ning" % "async-http-client" % "1.8.14",
          "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
          "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
          "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
          "org.scalatest" %% "scalatest" % "2.2.2" % "test",
          "ch.qos.logback" % "logback-classic" % "1.1.2" % "test",
          "org.slf4j" % "jcl-over-slf4j" % slf4jVersion % "test" intransitive(),
          "org.slf4j" % "log4j-over-slf4j" % slf4jVersion % "test" intransitive(),
          "org.slf4j" % "jul-to-slf4j" % slf4jVersion % "test" intransitive()
        )
      )
      .dependsOn(models)
  }

  // -------------------------------------------------------
  // Play-JSON-formats
  // -------------------------------------------------------
  lazy val playJsonFormats = Project(id = "play-json-formats", base = file("play-json-formats"), settings = nonPlayAppSettings)
    .settings(
      libraryDependencies ++= Seq(
        ws, //TODO when upgrading to Play 2.4; change this to use just play-json
        "org.scalatest" %% "scalatest" % "2.2.2" % "test",
        "org.scalatestplus" %% "play" % "1.2.0" % "test"
      ),
      name := "octoparts-play-json-formats",
      crossScalaVersions := Seq("2.10.4", theScalaVersion),
      crossVersion := CrossVersion.binary
    )
    .dependsOn(models)

  // -------------------------------------------------------
  // Scala-WS-client
  // -------------------------------------------------------
  lazy val scalaWsClient = Project(id = "scala-ws-client", base = file("scala-ws-client"), settings = nonPlayAppSettings)
    .settings(
      libraryDependencies ++= Seq(
        ws,
        "org.scalatest" %% "scalatest" % "2.2.2" % "test",
        "org.scalatestplus" %% "play" % "1.2.0" % "test"
      ),
      name := "octoparts-scala-ws-client",
      crossScalaVersions := Seq("2.10.4", theScalaVersion),
      crossVersion := CrossVersion.binary
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

