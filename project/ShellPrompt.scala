import sbt._
import scala.language.postfixOps

object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) = {}
    def error (s: => String) = {}
    def buffer[T] (f: => T) = f
  }
  def currBranch = {
    val maybeBranch = ("git symbolic-ref --short HEAD" lines_! devnull headOption)
    maybeBranch getOrElse "-"
  }

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      s"$currProject:$currBranch:${Version.octopartsVersion}> "
    }
  }
}