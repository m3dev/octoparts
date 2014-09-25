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

import play.PlayImport.PlayKeys._
import play.Play.autoImport._
import play._

import scala.language.postfixOps
import scalariform.formatter.preferences._

object OctopartsBuild extends Build {

  val octopartsVersion = "2.1-SNAPSHOT"

  val httpPort = 9000
  val theScalaVersion = "2.11.2"
  val thePlayVersion = "2.3.4" // make play-json-formats subproject depend on play-json when bumping to 2.4
  val slf4jVersion = "1.7.7"
  val hystrixVersion = "1.3.17"
  val httpClientVersion = "4.3.5"
  val scalikejdbcVersion = "2.1.1"
  val swaggerVersion = "1.3.8"
  val jacksonVersion = "2.4.2"

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
    scoverageSettings

  /*
   * Settings that are common for every project _except_ the Play app
   * (because we don't want to publish the Play app to Maven Central)
   */
  lazy val nonPlayAppSettings =
    commonSettings ++
    publishSettings

  /*
   * Settings for the Play app
   */
  lazy val playAppSettings =
      commonSettings ++
      playSettings ++
      buildInfoSettings ++
      buildInfoStuff ++
      sonatypeSettings ++
      coverallsSettings ++
      Seq(
        publishArtifact := false,
        libraryDependencies ++= Seq(
          // Logging
          "ch.qos.logback" % "logback-classic" % "1.1.2",
          "org.slf4j" % "slf4j-api" % slf4jVersion,
          "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
          "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
          "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
          "net.kencochrane.raven" % "raven-logback" % "5.0.1",
          "org.codehaus.janino" % "janino" % "2.7.5",

          // Hystrix
          "com.netflix.hystrix" % "hystrix-core" % hystrixVersion,
          "com.netflix.hystrix" % "hystrix-metrics-event-stream" % hystrixVersion,
          "com.netflix.rxjava" % "rxjava-scala" % "0.20.1", // matches version used in hystrix-core

          // Apache HTTP client
          "org.apache.httpcomponents" % "httpclient" % httpClientVersion,
          "org.apache.httpcomponents" % "httpclient-cache" % httpClientVersion,

          // DB
          "org.postgresql" % "postgresql" % "9.3-1102-jdbc41" % "runtime",
          "org.skinny-framework" %% "skinny-orm" % "1.3.1",
          "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
          "org.scalikejdbc" %% "scalikejdbc-play-plugin" % "2.3.1",
          "org.apache.commons" % "commons-dbcp2" % "2.0.1",

          // Memcached
          "com.bionicspirit" %% "shade" % "1.6.0",
          "net.spy" % "spymemcached" % "2.11.4",

          // Misc utils
          "commons-validator" % "commons-validator" % "1.4.0" % "runtime",
          "javax.transaction" % "jta" % "1.1",
          "com.netaporter" %% "scala-uri" % "0.4.2",

          // Play plugins
          "com.github.tototoshi" %% "play-flyway" % "1.1.2",
          "org.scaldi" %% "scaldi-play" % "0.4.1",
          "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.6",
          "com.wordnik" %% "swagger-play2" % swaggerVersion,

          // Test
          "com.typesafe.play" %% "play-test" % thePlayVersion % "test",
          "org.scalatest" %% "scalatest" % "2.2.2" % "test",
          "org.scalatestplus" %% "play" % "1.2.0" % "test",
          "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
          "org.codehaus.groovy" % "groovy" % "2.3.6" % "test",
          "org.scalikejdbc" %% "scalikejdbc-test"   % scalikejdbcVersion % "test"
        ).map(_.excludeAll(
          ExclusionRule(organization = "spy", name = "spymemcached"), // spymemcached's org changed from spy to net.spy
          ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
          ExclusionRule(organization = "org.slf4j", name = "slf4j-jdk14"),
          ExclusionRule(organization = "org.slf4j", name = "slf4j-jcl"),
          ExclusionRule(organization = "org.slf4j", name = "slf4j-nop"),
          ExclusionRule(organization = "org.slf4j", name = "slf4j-simple")))
      )

  lazy val playSettings = Seq(
    playVersion := thePlayVersion,
    playDefaultPort := httpPort
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

  lazy val scoverageSettings =
    instrumentSettings ++
    Seq(
      ScoverageKeys.highlighting := true,
      ScoverageKeys.excludedPackages in ScoverageCompile := """com\.kenshoo.*;.*controllers\.javascript\..*;.*controllers\.ref\..*;.*controllers\.Reverse.*;.*BuildInfo.*;.*views\.html\..*;Routes""",
      testOptions in ScoverageTest += Tests.Argument("-u", "target/test-reports")
    )

  lazy val formatterPrefs = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
  )

  lazy val buildInfoStuff = Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "com.m3.octoparts",
    buildInfoKeys := Seq[BuildInfoKey](
      "name" -> "Octoparts",
      version,
      scalaVersion,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      },
      "gitBranch" -> git.gitCurrentBranch.value,
      "gitTags" -> git.gitCurrentTags.value,
      "gitHEAD" -> git.gitHeadCommit.value
    )
  )

  // -------------------------------------------------------
  // Interface for authentication plugins
  // -------------------------------------------------------
  lazy val authPluginApi = Project(id = "auth-plugin-api", base = file("plugins/auth-plugin-api"), settings = nonPlayAppSettings).settings(
    name := "octoparts-auth-plugin-api",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % thePlayVersion % "provided"
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
      crossScalaVersions := Seq("2.10.4", "2.11.2"),
      crossVersion := CrossVersion.binary
    )


  // -------------------------------------------------------
  // Java client
  // -------------------------------------------------------
  lazy val javaClient = {
    Project(id = "java-client", base = file("java-client"), settings = nonPlayAppSettings)
      .settings(
        name := "octoparts-java-client",
        crossScalaVersions := Seq("2.10.4", "2.11.2"),
        javacOptions in compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint"),
        javacOptions in doc ++= Seq("-source", "1.6"),

        libraryDependencies ++= Seq(
          "com.google.code.findbugs" % "jsr305" % "3.0.0" intransitive(),
          "org.slf4j" % "slf4j-api" % slf4jVersion,
          "com.ning" % "async-http-client" % "1.8.13",
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
        "org.scalatest" %% "scalatest" % "2.2.1" % "test",
        "org.scalatestplus" %% "play" % "1.2.0" % "test"
      ),
      name := "octoparts-play-json-formats",
      crossScalaVersions := Seq("2.10.4", "2.11.2"),
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
        "org.scalatest" %% "scalatest" % "2.2.1" % "test",
        "org.scalatestplus" %% "play" % "1.2.0" % "test"
      ),
      name := "octoparts-scala-ws-client",
      crossScalaVersions := Seq("2.10.4", "2.11.2"),
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

  // Settings for publishing to Maven Central
  lazy val publishSettings = Seq(
    pomExtra :=
      <url>https://github.com/m3dev/octoparts</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:m3dev/octoparts.git</url>
        <connection>scm:git:git@github.com:m3dev/octoparts.git</connection>
      </scm>
      <developers>
        <developer>
          <id>lloydmeta</id>
          <name>Lloyd Chan</name>
          <url>https://github.com/lloydmeta</url>
        </developer>
        <developer>
          <id>cb372</id>
          <name>Chris Birchall</name>
          <url>https://github.com/cb372</url>
        </developer>
        <developer>
          <id>mauhiz</id>
          <name>Vincent PÉRICART</name>
          <url>https://github.com/mauhiz</url>
        </developer>
      </developers>,
    publishTo <<= version { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }
  )

}

