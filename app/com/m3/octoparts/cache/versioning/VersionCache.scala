package com.m3.octoparts.cache.versioning

import com.m3.octoparts.cache.versioning.LatestVersionCache._
import com.twitter.zipkin.gen.Span

import scala.concurrent.{ ExecutionContext, Future }

// this is more than a CacheKey since it has methods to talk to internal and external caches.
abstract class VersionCache[T](id: T)(implicit executionContext: ExecutionContext) {

  def getId = id

  def pollVersion(implicit parentSpan: Span): Future[Option[Version]]

  def doInsertExternal(version: Version)(implicit parentSpan: Span): Future[Unit]

  def knownVersion(implicit parentSpan: Span): Option[Version]

  def updateVersion(version: Version)(implicit parentSpan: Span): Unit

  def newLookup(implicit parentSpan: Span) = new VersionLookup(this)

  def insertNewVersion()(implicit parentSpan: Span): Future[Unit] = {
    val newVersion = LatestVersionCache.makeNewVersion
    updateVersion(newVersion)
    doInsertExternal(newVersion)
  }

}