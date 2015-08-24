package com.m3.octoparts.repository

import com.m3.octoparts.model.config._
import com.twitter.zipkin.gen.Span

import scala.collection.SortedSet
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
   * Find a single [[HttpPartConfig]] by [[HttpPartConfig.partId]]
   */
  def findConfigByPartId(partId: String)(implicit parentSpan: Span): Future[Option[HttpPartConfig]]

  /**
   * Returns all the configs from the repository, sorted by part_id.
   * Dependent [[HttpPartConfig.hystrixConfig]], [[HystrixConfig.threadPoolConfig]], [[HttpPartConfig.cacheGroups]], [[HttpPartConfig.parameters]], [[PartParam.cacheGroups]] are populated
   */
  def findAllConfigs()(implicit parentSpan: Span): Future[SortedSet[HttpPartConfig]]

  /**
   * Find a single [[PartParam]] by [[PartParam.id]]. Dependent [[PartParam.httpPartConfig]] and [[PartParam.cacheGroups]] are populated
   */
  def findParamById(id: Long)(implicit parentSpan: Span): Future[Option[PartParam]]

  /**
   * Find a single [[ThreadPoolConfig]] by [[ThreadPoolConfig.id]]. Dependent [[ThreadPoolConfig.hystrixConfigs]] and [[HystrixConfig.httpPartConfig]] are populated
   */
  def findThreadPoolConfigById(id: Long)(implicit parentSpan: Span): Future[Option[ThreadPoolConfig]]

  /**
   * Return all the thread pool configs, sorted by key. Dependent [[ThreadPoolConfig.hystrixConfigs]] and [[HystrixConfig.httpPartConfig]] are populated
   */
  def findAllThreadPoolConfigs()(implicit parentSpan: Span): Future[SortedSet[ThreadPoolConfig]]

  /**
   * Returns a single [[CacheGroup]] by [[CacheGroup.name]]. Dependent [[CacheGroup.httpPartConfigs]], [[CacheGroup.partParams]], and [[PartParam.httpPartConfig]] are populated
   */
  def findCacheGroupByName(name: String)(implicit parentSpan: Span): Future[Option[CacheGroup]]

  /**
   * Return all cache groups, sorted by name. Dependent [[CacheGroup.httpPartConfigs]], [[CacheGroup.partParams]], and [[PartParam.httpPartConfig]] are populated
   */
  def findAllCacheGroups()(implicit parentSpan: Span): Future[SortedSet[CacheGroup]]

  /**
   * Return all cache groups with the given names. Dependent [[CacheGroup.httpPartConfigs]], [[CacheGroup.partParams]], and [[PartParam.httpPartConfig]] are populated
   */
  def findAllCacheGroupsByName(name: String*)(implicit parentSpan: Span): Future[SortedSet[CacheGroup]]

}
