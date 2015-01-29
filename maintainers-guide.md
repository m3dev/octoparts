## Octoparts Maintainers' Guide

### Versioning Policy

```
"{server-major}.{server-minor}.{client-minor}"
```

When the "{client-minor}" is zero, version should be "{server-major}.{server-minor}".

### Release

#### Required

- Sonatype release account
- $HOME/.sbt/0.13/sonatype.sbt

``` scala
credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", "xxx", "yyy")
```

- $HOME/.sbt/0.13/plugins/gpg.sbt

```scala
// Use latest version
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
```

#### Operations

- Run `git flow release start/finish "{full-version}"`
- Run `./scripts/publish_libs.sh`
- Use `sbt sonatypeRelease` from `sbt-sonatype` plugin or access sonatype console (https://oss.sonatype.org/)
- Set version as "{next-version}-SNAPSHOT" on develop branch
- Update documentation (https://github.com/m3dev/octoparts-site/blob/develop/data/versions.yml)
