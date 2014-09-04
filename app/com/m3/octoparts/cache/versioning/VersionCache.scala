package com.m3.octoparts.cache.versioning

import com.m3.octoparts.cache.versioning.LatestVersionCache._

import scala.concurrent.{ ExecutionContext, Future }

// this is more than a CacheKey since it has methods to talk to internal and external caches.
abstract class VersionCache[T](id: T)(implicit executionContext: ExecutionContext) {

  def getId = id

  def pollVersion: Future[Option[Version]]

  def doInsertExternal(version: Version): Future[Unit]

  def knownVersion: Option[Version]

  def updateVersion(version: Version): Unit

  def newLookup = new VersionLookup(this)

  def insertNewVersion(): Future[Unit] = {
    val newVersion = LatestVersionCache.makeNewVersion
    updateVersion(newVersion)
    doInsertExternal(newVersion)
  }

}