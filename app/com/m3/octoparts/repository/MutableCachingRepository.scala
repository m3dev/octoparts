package com.m3.octoparts.repository

import com.m3.octoparts.cache.Cache
import com.m3.octoparts.cache.key.HttpPartConfigCacheKey
import com.m3.octoparts.http.HttpClientPool
import com.m3.octoparts.model.config.{ json, ConfigModel }
import com.m3.octoparts.repository.config.ConfigMapper
import com.twitter.zipkin.gen.Span

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Caching decorators for a mutable repository.
 */
class MutableCachingRepository(
  val delegate: MutableConfigsRepository,
  val cache: Cache,
  val httpClientPool: HttpClientPool)(
    implicit val executionContext: ExecutionContext)
    extends CachingRepository
    with MutableConfigsRepository {

  def save[A <: ConfigModel[A]: ConfigMapper](obj: A)(implicit parentSpan: Span): Future[Long] = reloadCacheAfter(delegate.save(obj))

  def deleteAllConfigs()(implicit parentSpan: Span): Future[Int] = reloadCacheAfter {
    for {
      configs <- findAllConfigs()
      deletedCount <- deleteAllConfigs() //This runs after findAllConfigs
    } yield {
      configs.foreach(c => put(HttpPartConfigCacheKey(c.partId), None))
      deletedCount
    }
  }

  def deleteConfigByPartId(partId: String)(implicit parentSpan: Span): Future[Int] = reloadCacheAfter {
    for {
      deletedCount <- delegate.deleteConfigByPartId(partId)
      _ <- put(HttpPartConfigCacheKey(partId), None)
    } yield deletedCount
  }

  def deleteThreadPoolConfigById(id: Long)(implicit parentSpan: Span): Future[Int] = reloadCacheAfter(delegate.deleteThreadPoolConfigById(id))

  def deletePartParamById(id: Long)(implicit parentSpan: Span): Future[Int] = reloadCacheAfter(delegate.deletePartParamById(id))

  def deleteCacheGroupByName(name: String)(implicit parentSpan: Span): Future[Int] = reloadCacheAfter(delegate.deleteCacheGroupByName(name))

  def importConfigs(configs: Seq[json.HttpPartConfig])(implicit parentSpan: Span): Future[Seq[String]] = reloadCacheAfter(delegate.importConfigs(configs))

  private def reloadCacheAfter[A](f: => Future[A])(implicit parentSpan: Span) = {
    for {
      result <- f
      reloadR <- reloadCache() // Don't worry, this happens after result
    } yield result
  }

}