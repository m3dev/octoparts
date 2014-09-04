package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.client.CacheClient
import com.m3.octoparts.cache.directive.CacheDirective
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.PartResponse

import scala.concurrent.Future

object NoCacheClient extends CacheClient {
  override def increasePartVersion(partId: String) = Future.successful(())

  /**
   * Lookup the item in the cache. If it is found, return it,
   * otherwise run the provided block, store the result in the cache and return it.
   *
   * @param directive cache directive
   * @param f block to generate a value if one is not found in the cache
   * @return a Future of the value
   */
  override def putIfAbsent(directive: CacheDirective)(f: => Future[PartResponse]) = f

  override def increaseParamVersion(vpk: VersionedParamKey) = Future.successful(())

  override def saveLater(partResponse: PartResponse, directive: CacheDirective) = Future.successful(())
}
