package com.m3.octoparts.support.mocks

import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.config.ConfigMapper
import com.m3.octoparts.repository.{ ConfigsRepository, MutableConfigsRepository }
import com.twitter.zipkin.gen.Span

import scala.concurrent.Future

/**
 * Reusable mock implementation of a ConfigsRepository
 */
trait MockConfigRepository extends ConfigsRepository with ConfigDataMocks {

  def findConfigByPartId(partId: String)(implicit parentSpan: Span): Future[Option[HttpPartConfig]] = Future.successful(Some(mockHttpPartConfig))

  def findAllConfigs()(implicit parentSpan: Span): Future[Seq[HttpPartConfig]] = Future.successful(Seq(mockHttpPartConfig))

  def findThreadPoolConfigById(id: Long)(implicit parentSpan: Span): Future[Option[ThreadPoolConfig]] = Future.successful(Some(mockThreadConfig))

  def findAllThreadPoolConfigs()(implicit parentSpan: Span): Future[Seq[ThreadPoolConfig]] = Future.successful(Seq(mockThreadConfig))

  def findAllCacheGroups()(implicit parentSpan: Span): Future[Seq[CacheGroup]] = Future.successful(Seq(mockCacheGroup))

  def findCacheGroupByName(name: String)(implicit parentSpan: Span): Future[Option[CacheGroup]] = Future.successful(Some(mockCacheGroup.copy(name = name)))

  def findParamById(id: Long)(implicit parentSpan: Span): Future[Option[PartParam]] = Future.successful(Some(mockPartParam.copy(id = Some(123))))

  def findAllCacheGroupsByName(names: String*)(implicit parentSpan: Span): Future[Seq[CacheGroup]] = Future.successful(Seq(mockCacheGroup))
}

/**
 * Reusable mock mutable repository
 */
trait MockMutableRepository extends MockConfigRepository with MutableConfigsRepository {

  def deleteConfigByPartId(partId: String)(implicit parentSpan: Span) = Future.successful(1)

  def deletePartParamById(id: Long)(implicit parentSpan: Span) = Future.successful(1)

  def deleteThreadPoolConfigById(id: Long)(implicit parentSpan: Span) = Future.successful(1)

  def deleteCacheGroupByName(name: String)(implicit parentSpan: Span) = Future.successful(1)

  def deleteAllConfigs()(implicit parentSpan: Span) = Future.successful(1)

  def save[A <: ConfigModel[A]: ConfigMapper](obj: A)(implicit parentSpan: Span): Future[Long] = Future.successful(123)

  def importConfigs(configs: Seq[json.HttpPartConfig])(implicit parentSpan: Span) = Future.successful(Seq.empty)

}