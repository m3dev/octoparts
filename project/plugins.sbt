// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Classpaths.sbtPluginReleases

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.6")

// scoverage for test coverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.9")

// to show transitive dependencies as a graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

// for code formatting
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// To check for outdated dependencies (run "sbt dependency-updates")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6")

// Generate a BuildInfo class
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.1")

// Access git from sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")

// For publishing to Sonatype OSS
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")

// For publishing coverage data to coveralls.io
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "0.99.0")
