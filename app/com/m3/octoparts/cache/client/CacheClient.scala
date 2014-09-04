package com.m3.octoparts.cache.client

import com.m3.octoparts.cache.directive.CacheDirective
import com.m3.octoparts.cache.versioning._
import com.m3.octoparts.model.PartResponse

import scala.concurrent.Future

/**
 * The client that performs actual caching operations
 */
trait CacheClient {
  def increasePartVersion(partId: String): Future[Unit]

  def increaseParamVersion(vpk: VersionedParamKey): Future[Unit]

  /**
   * Lookup the item in the cache. If it is found, return it,
   * otherwise run the provided block, store the result in the cache and return it.
   *
   * @param directive cache directive
   * @param f block to generate a value if one is not found in the cache
   * @return a Future of the value
   */
  def putIfAbsent(directive: CacheDirective)(f: => Future[PartResponse]): Future[PartResponse]

  /**
   * Inconditional put
   *
   * @param directive
   * @param partResponse
   * @return
   */
  def saveLater(partResponse: PartResponse, directive: CacheDirective): Future[Unit]
}

