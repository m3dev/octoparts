// The Typesafe repository
resolvers ++= Seq(
  "jgit-repo"           at "http://download.eclipse.org/jgit/maven",
  Classpaths.sbtPluginReleases
)

// The Play plugin
// TODO: upgrading to 2.5
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.8")
// scoverage for test coverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.4.0")
// to show transitive dependencies as a graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")
// for code formatting
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
// To check for outdated dependencies (run "sbt dependency-updates")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.2.0")
// Generate a BuildInfo class
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")
// Access git from sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
// For publishing to Sonatype OSS
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
// For publishing coverage data to coveralls.io
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.1.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")
