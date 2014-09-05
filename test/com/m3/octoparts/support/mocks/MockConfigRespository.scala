package com.m3.octoparts.support.mocks

import com.m3.octoparts.repository.config.ConfigMapper
import com.m3.octoparts.repository.{ MutableConfigsRepository, ConfigsRepository }
import com.m3.octoparts.model.config._
import scala.concurrent.Future

/**
 * Reusable mock implementation of a ConfigsRepository
 */
trait MockConfigRespository extends ConfigsRepository with ConfigDataMocks {
  def findConfigByPartId(partId: String): Future[Option[HttpPartConfig]] = Future.successful(Some(mockHttpPartConfig))

  def findAllConfigs(): Future[Seq[HttpPartConfig]] = Future.successful(Seq(mockHttpPartConfig))

  def findThreadPoolConfigById(id: Long): Future[Option[ThreadPoolConfig]] = Future.successful(Some(mockThreadConfig))

  def findAllThreadPoolConfigs(): Future[Seq[ThreadPoolConfig]] = Future.successful(Seq(mockThreadConfig))

  def findAllCacheGroups(): Future[Seq[CacheGroup]] = Future.successful(Seq(mockCacheGroup))

  def findCacheGroupByName(name: String): Future[Option[CacheGroup]] = Future.successful(Some(mockCacheGroup.copy(name = name)))

  def findParamById(id: Long): Future[Option[PartParam]] = Future.successful(Some(mockPartParam.copy(id = Some(123))))

  def findAllCacheGroupsByName(names: String*): Future[Seq[CacheGroup]] = Future.successful(Seq(mockCacheGroup))
}

/**
 * Reusable mock mutable repository
 */
trait MockMutableRepository extends MockConfigRespository with MutableConfigsRepository {
  def deleteConfigByPartId(partId: String) = Future.successful(1)

  def deletePartParamById(id: Long) = Future.successful(1)

  def deleteThreadPoolConfigById(id: Long) = Future.successful(1)

  def deleteCacheGroupByName(name: String) = Future.successful(1)

  def deleteAllConfigs() = Future.successful(1)

  def save[A <: ConfigModel[A]: ConfigMapper](obj: A): Future[Long] = Future.successful(123)

}