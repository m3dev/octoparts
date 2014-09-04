package com.m3.octoparts.cache.versioning

import scala.collection.concurrent.TrieMap

/**
 * A simple implementation of [[LatestVersionCache]] that holds the latest versions as Maps
 */
class InMemoryLatestVersionCache extends LatestVersionCache {

  import com.m3.octoparts.cache.versioning.LatestVersionCache._

  private val partVersions = new TrieMap[PartId, Version]
  private val paramVersions = new TrieMap[VersionedParamKey, Version]

  override def updatePartVersion(partId: PartId, version: Version): Unit = {
    partVersions.put(partId, version)
  }

  override def updateParamVersion(versionedParamKey: VersionedParamKey, version: Version): Unit = {
    paramVersions.put(versionedParamKey, version)
  }

  override def getPartVersion(partId: PartId) = partVersions.get(partId)

  override def getParamVersion(versionedParamKey: VersionedParamKey) = paramVersions.get(versionedParamKey)

}
