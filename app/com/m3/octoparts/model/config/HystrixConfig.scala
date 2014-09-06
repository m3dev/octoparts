package com.m3.octoparts.model.config

import scala.concurrent.duration._
import org.joda.time.DateTime

import scala.language.postfixOps

object HystrixConfig {
  val defaultTimeout = (5 seconds).toMillis
}

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
    timeoutInMs: Long = HystrixConfig.defaultTimeout,
    createdAt: DateTime,
    updatedAt: DateTime) extends ConfigModel[HystrixConfig] {

  /**
   * Method to use when we are sure we have a ThreadPoolConfig inside the
   * threadPoolConfig field. Throws an error if it's empty
   */
  def threadPoolConfigItem: ThreadPoolConfig = threadPoolConfig.get

}
