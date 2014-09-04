package com.m3.octoparts.hystrix

import com.m3.octoparts.model.config.HystrixConfig
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommand.Setter

import scala.concurrent.Future

/**
 * Companion object for instantiating HystrixExecutors
 */
object HystrixExecutor extends HystrixSetterSupport {

  /**
   * Instantiates and returns a HystrixExecutor that you can call #future on, passing
   * in a block you want to run asynchronously and backed by Hystrix
   */
  def apply(hystrixConfig: HystrixConfig): HystrixExecutor = new HystrixExecutor(setter(hystrixConfig))
}

/**
 * This should be instantiated via the factory method in the companion object.
 *
 * Serves to simplify wrapping any synchronous piece of code inside
 * a HystrixCommand. Mostly delegates the work to Hystrix
 *
 * @param setter HystrixCommand Setter.
 */
class HystrixExecutor(setter: Setter) {

  /**
   * Returns a Future result of the block passed, run within the context
   * of the Hystrix configuration settings that this HystrixExecutor object
   * was instantiated with.
   *
   * @return Future[R]
   */
  def future[T](f: => T): Future[T] = {
    new HystrixCommand[T](setter) with HystrixFutureSupport[T] {
      override def run = f
    }.future
  }
}