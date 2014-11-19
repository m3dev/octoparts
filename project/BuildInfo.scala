import com.typesafe.sbt.SbtGit.git
import sbt._
import sbt.Keys._
import sbtbuildinfo.Plugin._

object BuildInfo {

  val config = Seq(
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

  val settings = buildInfoSettings ++ config
}