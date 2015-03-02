package com.m3.octoparts.cache.versioning

import com.m3.octoparts.cache.versioning.LatestVersionCache._
import com.twitter.zipkin.gen.Span

import scala.concurrent.{ ExecutionContext, Future }

class VersionLookup[T](val versionCache: VersionCache[T])(implicit executionContext: ExecutionContext, parentSpan: Span) {

  import com.m3.octoparts.cache.versioning.FutureUtil._

  val externalVersion: Future[Option[Version]] = versionCache.pollVersion
  val internalVersion: Option[Version] = versionCache.knownVersion

  final def doUpdateLocal(version: Version) = versionCache.updateVersion(version)

  // if external version is present, force update the internal version
  final def updateLocal(): Future[Unit] = doIfWasSome(externalVersion)(doUpdateLocal)

  // if external version is missing, make a new one and insert in both.
  final def insertExternal(): Future[Boolean] = thenDoIfWasNone(externalVersion)(versionCache.insertNewVersion())

  // version must be defined and matching the known one
  final def willMatch: Future[Boolean] = externalVersion.map {
    version =>
      version.fold(false)(internalVersion.contains)
  }

}
