package com.m3.octoparts.hystrix

import com.m3.octoparts.model.config.HystrixConfig
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
    val threadPoolProperties = HystrixThreadPoolProperties.Setter().
      withCoreSize(config.threadPoolConfigItem.coreSize).
      withMaxQueueSize(config.threadPoolConfigItem.queueSize)

    HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(config.commandGroupKey)).
      andCommandKey(HystrixCommandKey.Factory.asKey(config.commandKey)).
      andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(config.threadPoolConfigItem.threadPoolKey)).
      andThreadPoolPropertiesDefaults(threadPoolProperties).
      andCommandPropertiesDefaults(HystrixCommandProperties.Setter().
        withExecutionTimeoutInMilliseconds(config.timeout.toMillis.toInt).
        withFallbackEnabled(config.localContentsAsFallback))
  }
}
