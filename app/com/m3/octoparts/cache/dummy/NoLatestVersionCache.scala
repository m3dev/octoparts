package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.versioning.LatestVersionCache.{ PartId, Version }
import com.m3.octoparts.cache.versioning.{ LatestVersionCache, VersionedParamKey }

object NoLatestVersionCache extends LatestVersionCache {
  override def getPartVersion(partId: PartId) = None

  /**
   * Update the latest known cache version for a given part
   */
  override def updatePartVersion(partId: PartId, version: Version) = Unit

  override def getParamVersion(versionedParamKeys: VersionedParamKey): Option[Version] = None

  /**
   * Update the latest known cache version for a given param value
   */
  override def updateParamVersion(versionedParamKey: VersionedParamKey, version: Version) = Unit
}
