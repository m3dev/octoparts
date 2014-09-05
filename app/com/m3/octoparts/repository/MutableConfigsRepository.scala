package com.m3.octoparts.repository

import com.m3.octoparts.model.config.ConfigModel
import com.m3.octoparts.repository.config.ConfigMapper

import scala.concurrent.Future

/**
 * Interface for ConfigRepositories that allow for changing or deleting
 * entities
 */
trait MutableConfigsRepository extends ConfigsRepository {
  /**
   * removes a config item from this repository
   */
  def deleteConfigByPartId(partId: String): Future[Int]

  /**
   * Deletes a PartParam
   */
  def deletePartParamById(id: Long): Future[Int]

  /**
   * Deletes a ThreadPoolConfig
   */
  def deleteThreadPoolConfigById(id: Long): Future[Int]

  /**
   * Deletes a CacheGroup by name
   */
  def deleteCacheGroupByName(name: String): Future[Int]

  /**
   * Deletes all configs
   * TODO: Decide if having this method is even a good idea
   */
  def deleteAllConfigs(): Future[Int]

  /**
   * Returns the id of the saved object.
   *
   * If the object has a None as id, returns a new id, otherwise it it has an id, its database records are updated
   * and the same id is returned.
   *
   * @return Long, the id of the model that was saved
   */
  def save[A <: ConfigModel[A]: ConfigMapper](obj: A): Future[Long]

}
