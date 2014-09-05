package com.m3.octoparts.model.config

import com.m3.octoparts.repository.config.ThreadPoolConfigRepository
import org.joda.time.DateTime

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

}