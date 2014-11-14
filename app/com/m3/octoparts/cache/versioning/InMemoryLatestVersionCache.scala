package com.m3.octoparts.cache.versioning

import com.google.common.cache.{ Cache, CacheBuilder }

/**
 * A simple implementation of [[LatestVersionCache]] that holds the latest versions as Maps
 */
class InMemoryLatestVersionCache(maxCacheKeys: Long) extends LatestVersionCache {

  import com.m3.octoparts.cache.versioning.LatestVersionCache._

  private[versioning] val partVersions = configureMemoryCache(CacheBuilder.newBuilder()).build[PartId, Object]()
  private[versioning] val paramVersions = configureMemoryCache(CacheBuilder.newBuilder()).build[VersionedParamKey, Object]()

  override def updatePartVersion(partId: PartId, version: Version): Unit = {
    partVersions.put(partId, version.asInstanceOf[Object])
  }

  override def updateParamVersion(versionedParamKey: VersionedParamKey, version: Version): Unit = {
    paramVersions.put(versionedParamKey, version.asInstanceOf[Object])
  }

  override def getPartVersion(partId: PartId) = getVersionFrom(partVersions)(partId)

  override def getParamVersion(versionedParamKey: VersionedParamKey) = getVersionFrom(paramVersions)(versionedParamKey)

  private def configureMemoryCache(builder: CacheBuilder[Object, Object]): CacheBuilder[Object, Object] = {
    builder.maximumSize(maxCacheKeys)
  }

  private def getVersionFrom[A](cache: Cache[A, Object])(key: A): Option[Version] = {
    val local = Option(cache.getIfPresent(key))
    local match {
      case Some(value) => Some(value.asInstanceOf[Version])
      case _ => None
    }
  }

}
