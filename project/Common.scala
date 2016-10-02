import com.typesafe.sbt.packager.Keys._

import sbt.Keys._
import sbt._
import net.virtualvoid.sbt.graph.Plugin._
import xerial.sbt.Sonatype._

import play.sbt.Play.autoImport._
import play.sbt.routes.RoutesKeys._
import PlayKeys._

object Common {

  /**
   * Settings that are common to every single project
   */
  lazy val commonSettings = Seq(
    initialCommands := "import com.m3.octoparts._",
    organization := "com.m3",
    version := Version.octopartsVersion,
    scalaVersion := Version.theScalaVersion,
    shellPrompt  := ShellPrompt.buildShellPrompt,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint"),
    updateOptions := updateOptions.value
      .withCircularDependencyLevel(CircularDependencyLevel.Error)
      .withCachedResolution(true)
  ) ++
    Dependencies.resolverSettings ++
    graphSettings ++
    compilerSettings ++
    ideSettings ++
    testSettings ++
    Scalariform.settings ++
    Scoverage.settings ++
    LintConfig.lintStuff

  /**
   * Settings that are common for every project _except_ the Play app
   * (because we don't want to publish the Play app to Maven Central)
   *
   * Also, the Play app doesn't get cross-compiled because there are a few
   * dependencies that are broken or didn't exist for 2.10
  */
  lazy val nonPlayAppSettings =
    commonSettings ++
    Publishing.settings ++
    Seq(
      crossScalaVersions := Seq("2.10.5", Version.theScalaVersion),
      crossVersion := CrossVersion.binary
    )

  /**
   * Settings for the (main) Play app
   */
  lazy val playAppSettings =
    commonSettings ++
    BuildInfo.settings ++
    sonatypeSettings ++
    Seq(
      routesGenerator := InjectedRoutesGenerator,
      playDefaultPort := 9000,
      publishArtifact := false,
      libraryDependencies ++= (Dependencies.rootDependencies ++ Dependencies.playScalatestDependencies)
    )

  private lazy val ideSettings = Seq(
    // Faster "sbt gen-idea"
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)
  )

  private lazy val compilerSettings = Seq(
    // the name-hashing algorithm for the incremental compiler.
    incOptions := incOptions.value.withNameHashing(nameHashing = true),

    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
  )

  private lazy val testSettings = Seq(
    parallelExecution in Test := false,  // Avoid DB-related tests stomping on each other
    testOptions in Test += Tests.Argument("-oF") // full stack traces
  )
}
