import com.typesafe.sbt.packager.Keys._
import play.PlayImport.PlayKeys._
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin
import net.virtualvoid.sbt.graph.Plugin._
import xerial.sbt.Sonatype._
import org.scoverage.coveralls.CoverallsPlugin.coverallsSettings

object Common {

  /**
   * Settings that are common to every single project
   */
  lazy val commonSettings = Seq(
    initialCommands := "import com.m3.octoparts._",
    organization := "com.m3",
    version := Version.octopartsVersion,
    scalaVersion := Version.theScalaVersion,
    shellPrompt  := ShellPrompt.buildShellPrompt
  ) ++
    Dependencies.resolverSettings ++
    graphSettings ++
    compilerSettings ++
    ideSettings ++
    testSettings ++
    Scalariform.settings ++
    Scoverage.settings

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
      crossScalaVersions := Seq("2.10.4", Version.theScalaVersion),
      crossVersion := CrossVersion.binary
    )

  /**
   * Settings for the (main) Play app
   */
  lazy val playAppSettings =
    commonSettings ++
    BuildInfo.settings ++
    addConfDirToClasspathSettings ++
    excludeConfFilesFromJarSettings ++
    sonatypeSettings ++
    coverallsSettings ++
    Seq(
      playVersion := Dependencies.thePlayVersion,
      playDefaultPort := 9000,
      publishArtifact := false,
      libraryDependencies ++= Dependencies.rootDependencies
    )


  private lazy val addConfDirToClasspathSettings = Seq(
    // Add the contents of the /conf dir of an unzipped tarball to the start of the classpath
    // so we can edit the config files of a deployed app.
    // See https://github.com/playframework/playframework/issues/3473
    scriptClasspath := "../conf" +: scriptClasspath.value
  )


  private lazy val excludeConfFilesFromJarSettings = Seq(
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

  private lazy val ideSettings = Seq(
    // Faster "sbt gen-idea"
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)
  )

  private lazy val compilerSettings = Seq(
    // the name-hashing algorithm for the incremental compiler.
    incOptions := incOptions.value.withNameHashing(nameHashing = true),

    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint")
  )

  private lazy val testSettings = Seq(Test, ScoverageSbtPlugin.ScoverageTest).flatMap { t =>
    Seq(
      parallelExecution in t := false,  // Avoid DB-related tests stomping on each other
      testOptions in t += Tests.Argument("-oF") // full stack traces
    )
  }
}