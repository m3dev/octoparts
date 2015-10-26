package com.m3.octoparts.hystrix

import com.m3.octoparts.model.config.{ ThreadPoolConfig, HystrixConfig }
import com.netflix.hystrix.HystrixThreadPoolProperties.Setter
import com.netflix.hystrix._

/**
 * Trait that allows us to easily convert between our own HystrixArgument case class into
 * a HystrixCommand.Setter
 */
trait HystrixSetterSupport {

  /**
   * Given a HystrixArgument case class returns a HystrixCommand.Setter object
   */
  def setter(config: HystrixConfig): HystrixCommand.Setter = {
    val threadPoolConfig: ThreadPoolConfig = config.threadPoolConfigItem
    val threadPoolProperties = threadPoolSetter(threadPoolConfig)

    HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(config.commandGroupKey)).
      andCommandKey(HystrixCommandKey.Factory.asKey(config.commandKey)).
      andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolConfig.threadPoolKey)).
      andThreadPoolPropertiesDefaults(threadPoolProperties).
      andCommandPropertiesDefaults(HystrixCommandProperties.Setter().
        withExecutionTimeoutInMilliseconds(config.timeout.toMillis.toInt).
        withFallbackEnabled(config.localContentsAsFallback))
  }

  private[hystrix] def threadPoolSetter(threadPoolConfig: ThreadPoolConfig): Setter = {
    HystrixThreadPoolProperties.Setter().
      withCoreSize(threadPoolConfig.coreSize).
      // Hystrix uses both of these for setting Queue size
      withMaxQueueSize(threadPoolConfig.queueSize).
      withQueueSizeRejectionThreshold(threadPoolConfig.queueSize)
  }
}
