import sbt._
import sbt.Keys._

object Publishing {

  val settings = Seq(
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
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }
  )
}
