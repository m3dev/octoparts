package com.m3.octoparts.model.config

import com.m3.octoparts.repository.config.HystrixConfigRepository
import org.joda.time.DateTime

import scala.language.postfixOps

/**
 * Holds the Hystrix Configuration data for a given dependency
 *
 * TODO: Link to a ThreadPoolConfig
 */
case class HystrixConfig(
    id: Option[Long] = None, // None means the HystrixConfig is new (not inserted yet)
    httpPartConfigId: Option[Long] = None,
    httpPartConfig: Option[HttpPartConfig] = None,
    threadPoolConfigId: Option[Long] = None,
    threadPoolConfig: Option[ThreadPoolConfig] = None,
    commandKey: String,
    commandGroupKey: String,
    timeoutInMs: Long = HystrixConfigRepository.defaultTimeout,
    createdAt: DateTime,
    updatedAt: DateTime) extends ConfigModel[HystrixConfig] {

  override def mapper = HystrixConfigRepository

  /**
   * Method to use when we are sure we have a ThreadPoolConfig inside the
   * threadPoolConfig field. Throws an error if it's empty
   */
  def threadPoolConfigItem: ThreadPoolConfig = threadPoolConfig.get

}
