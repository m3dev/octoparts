import sbt.Keys._
import sbt._

object LintConfig {

  private def lintEnv = sys.props.getOrElse("OCTOPARTS_LINT", sys.env.getOrElse("OCTOPARTS_LINT", "false")).toBoolean

  val doLint = settingKey[Boolean]("doLint")

  val lintStuff: Seq[Def.Setting[_]] = Seq(
    doLint := lintEnv,
    libraryDependencies ++= {
      if (doLint.value) {
        val scapegoat = "com.sksamuel.scapegoat" %% "scalac-scapegoat-plugin" % "0.94.6"
        Seq(
          compilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17"),
          scapegoat % Provided,
          compilerPlugin(scapegoat)
        )
      } else Nil
    },
    resolvers += "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases",
    scalacOptions += "-Xlint",
    scalacOptions ++= {
     if (doLint.value) Seq(
       "-Ywarn-dead-code",
       "-Ywarn-numeric-widen",
       "-Ywarn-unused",
       "-Ywarn-unused-import",
       "-Ywarn-value-discard",
       "-P:scapegoat:consoleOutput:true",
       s"-P:scapegoat:dataDir:${(crossTarget in Compile).value.getAbsolutePath}/scapegoat-report",
       s"-P:scapegoat:ignoredFiles:.*\\${java.io.File.separator}target\\${java.io.File.separator}.*"
     )
     else Nil
    })
}
