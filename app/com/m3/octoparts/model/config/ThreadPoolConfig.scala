package com.m3.octoparts.model.config

import org.joda.time.DateTime
import com.m3.octoparts.model.config.json.{ ThreadPoolConfig => JsonThreadPoolConfig }

/**
 * Holds ThreadPool Configuration data. Mostly used for Hystrix
 */
case class ThreadPoolConfig(
    id: Option[Long] = None, // None -> new
    threadPoolKey: String,
    coreSize: Int = ThreadPoolConfig.defaultCoreSize,
    hystrixConfigs: Seq[HystrixConfig] = Seq.empty,
    createdAt: DateTime,
    updatedAt: DateTime) extends ConfigModel[ThreadPoolConfig] {

  // this setting is not yet available for users
  def queueSize: Int = ThreadPoolConfig.defaultQueueSize

}

object ThreadPoolConfig {

  val defaultCoreSize = 2
  val defaultQueueSize = 256

  /**
   * Returns a [[JsonThreadPoolConfig]] for a [[ThreadPoolConfig]]
   */
  def toJsonModel(config: ThreadPoolConfig): JsonThreadPoolConfig = {
    JsonThreadPoolConfig(
      threadPoolKey = config.threadPoolKey,
      coreSize = config.coreSize,
      queueSize = config.queueSize)
  }

  def fromJsonModel(config: JsonThreadPoolConfig): ThreadPoolConfig = {
    ThreadPoolConfig(
      threadPoolKey = config.threadPoolKey,
      coreSize = config.coreSize,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }

}