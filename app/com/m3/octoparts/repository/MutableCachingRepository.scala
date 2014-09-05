package com.m3.octoparts.repository

import com.m3.octoparts.cache.CacheCodecs
import com.m3.octoparts.cache.client.CacheAccessor
import com.m3.octoparts.cache.key.HttpPartConfigCacheKey
import com.m3.octoparts.http.HttpClientPool
import com.m3.octoparts.model.config.{ HttpPartConfig, ConfigModel }
import CacheCodecs._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Caching decorators for a mutable repository.
 */
class MutableCachingRepository(
  val delegate: MutableConfigsRepository,
  val cacheAccessor: CacheAccessor,
  val httpClientPool: HttpClientPool)(
    implicit val executionContext: ExecutionContext)
    extends CachingRepository
    with MutableConfigsRepository {

  def save[A <: ConfigModel[A]](obj: A): Future[Long] = reloadCacheAfter(delegate.save(obj))

  def deleteAllConfigs(): Future[Int] = reloadCacheAfter {
    for {
      configs <- findAllConfigs()
      deletedCount <- deleteAllConfigs() //This runs after findAllConfigs
    } yield {
      configs.foreach(c => put[Option[HttpPartConfig]](HttpPartConfigCacheKey(c.partId), None))
      deletedCount
    }
  }

  def deleteConfigByPartId(partId: String): Future[Int] = reloadCacheAfter {
    for {
      deletedCount <- delegate.deleteConfigByPartId(partId)
      _ <- put[Option[HttpPartConfig]](HttpPartConfigCacheKey(partId), None)
    } yield deletedCount
  }

  def deleteThreadPoolConfigById(id: Long): Future[Int] = reloadCacheAfter(delegate.deleteThreadPoolConfigById(id))

  def deletePartParamById(id: Long): Future[Int] = reloadCacheAfter(delegate.deletePartParamById(id))

  def deleteCacheGroupByName(name: String): Future[Int] = reloadCacheAfter(delegate.deleteCacheGroupByName(name))

  private def reloadCacheAfter[A](f: => Future[A]) = {
    for {
      result <- f
      reloadR <- reloadCache() // Don't worry, this happens after result
    } yield result
  }

}