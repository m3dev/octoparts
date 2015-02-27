package com.m3.octoparts.hystrix

import com.m3.octoparts.model.config.HttpPartConfig
import com.netflix.hystrix.HystrixCommand

import scala.concurrent.Future

case class HystrixExecutor(config: HttpPartConfig) extends HystrixSetterSupport {

  /**
   * Returns a Future result of the block passed, run within the context
   * of the Hystrix configuration settings that this HystrixExecutor object
   * was instantiated with.
   *
   * Note that if withFallback is passed, it will always be used.
   *
   * @param f The function to be made async
   * @param fallbackTransform How to transform the local contents into a fallback.
   *                          This will not be called if the HystrixConfig in HttpPartConfig has
   *                          localContentsAsFallback set to false
   */
  def future[T](f: => T, fallbackTransform: Option[String] => T): Future[T] = {
    new HystrixCommand[T](setter(config.hystrixConfigItem)) with HystrixFutureSupport[T] {
      def run = f
      override def getFallback: T = fallbackTransform(config.localContents)
    }.future
  }
}