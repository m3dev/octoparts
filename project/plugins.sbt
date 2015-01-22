// The Typesafe repository
resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "jgit-repo"           at "http://download.eclipse.org/jgit/maven",
  Classpaths.sbtPluginReleases
)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")
// scoverage for test coverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.1")
// to show transitive dependencies as a graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
// for code formatting
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")
// To check for outdated dependencies (run "sbt dependency-updates")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6")
// Generate a BuildInfo class
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")
// Access git from sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")
// For publishing to Sonatype OSS
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")
// For publishing coverage data to coveralls.io
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.0.0.BETA1")