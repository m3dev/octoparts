import sbt.Keys._
import sbt._

object LintConfig {

  private def lintEnv = sys.props.getOrElse("OCTOPARTS_LINT", sys.env.getOrElse("OCTOPARTS_LINT", "false")).toBoolean

  val doLint = settingKey[Boolean]("doLint")

  val lintStuff: Seq[Def.Setting[_]] = Seq(
    doLint := lintEnv,
    libraryDependencies <++= doLint(lintDependencies),
    resolvers += "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases",
    scalacOptions <++= (crossTarget in Compile, doLint)(lintScalacOptions) map identity
  )

  private def lintScalacOptions(crossTargetFile: File, lint: Boolean): Seq[String] = {
    Seq("-Xlint") ++ (if (lint) Seq(
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-value-discard",
      "-P:scapegoat:consoleOutput:true",
      s"-P:scapegoat:dataDir:${crossTargetFile.getAbsolutePath}/scapegoat-report",
      s"-P:scapegoat:ignoredFiles:.*\\${java.io.File.separator}target\\${java.io.File.separator}.*"
    )
    else Nil)
  }

  private def lintDependencies(lint: Boolean): Seq[ModuleID] = {
    if (lint) {
      val scapegoat = "com.sksamuel.scapegoat" %% "scalac-scapegoat-plugin" % "0.94.6"
      Seq(
        compilerPlugin("com.foursquare.lint" %% "linter" % "0.1-SNAPSHOT"),
        scapegoat % Provided,
        compilerPlugin(scapegoat)
      )
    } else Nil
  }
}
