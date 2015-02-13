package com.m3.octoparts.model.config

import scala.concurrent.duration._
import org.joda.time.DateTime
import com.m3.octoparts.model.config.json.{ HystrixConfig => JsonHystrixConfig }

import scala.language.postfixOps

object HystrixConfig {
  val defaultTimeout = (5 seconds).toMillis

  /**
   * Returns a [[JsonHystrixConfig]] for a given [[HystrixConfig]]
   */
  def toJsonModel(config: HystrixConfig): JsonHystrixConfig = {
    require(config.threadPoolConfig.isDefined)
    JsonHystrixConfig(
      threadPoolConfig = ThreadPoolConfig.toJsonModel(config.threadPoolConfig.get),
      commandKey = config.commandKey,
      commandGroupKey = config.commandGroupKey,
      localContentsAsFallback = config.localContentsAsFallback,
      timeout = config.timeoutInMs.millis
    )
  }

  def fromJsonModel(config: JsonHystrixConfig): HystrixConfig = {
    HystrixConfig(
      threadPoolConfig = Some(ThreadPoolConfig.fromJsonModel(config.threadPoolConfig)),
      commandKey = config.commandKey,
      commandGroupKey = config.commandGroupKey,
      timeoutInMs = config.timeout.toMillis,
      localContentsAsFallback = config.localContentsAsFallback,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }
}

/**
 * Holds the Hystrix Configuration data for a given dependency
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
    localContentsAsFallback: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime) extends ConfigModel[HystrixConfig] {

  /**
   * Method to use when we are sure we have a ThreadPoolConfig inside the
   * threadPoolConfig field. Throws an error if it's empty
   */
  def threadPoolConfigItem: ThreadPoolConfig = threadPoolConfig.get

}