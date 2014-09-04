package com.m3.octoparts.repository

import com.m3.octoparts.model.config._

import scala.concurrent.Future

/**
 * A ConfigsRepository is an abstract DAO that makes no assumptions about
 * the persistence-layer that may implement it
 *
 * This is useful so that we can decorate all calls to the actual persistence
 * layers, for example with caching.
 */
trait ConfigsRepository {
  /**
   * Wrapped version of a standard get operation that works much like Map#get
   */
  def findConfigByPartId(partId: String): Future[Option[HttpPartConfig]]

  /**
   * Returns all the configs from the repository
   */
  def findAllConfigs(): Future[Seq[HttpPartConfig]]

  def findParamById(id: Long): Future[Option[PartParam]]

  /**
   * Find a single ThreadPoolConfig by id
   */
  def findThreadPoolConfigById(id: Long): Future[Option[ThreadPoolConfig]]

  /**
   * Return all the thread pool configs
   */
  def findAllThreadPoolConfigs(): Future[Seq[ThreadPoolConfig]]

  /**
   * Returns a single cache group by name
   */
  def findCacheGroupByName(name: String): Future[Option[CacheGroup]]

  /**
   * Return all cache groups
   */
  def findAllCacheGroups(): Future[Seq[CacheGroup]]

  /**
   * Return all cache groups with the given names
   */
  def findAllCacheGroupsByName(name: String*): Future[Seq[CacheGroup]]

}
