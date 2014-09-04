package com.m3.octoparts.hystrix

import com.m3.octoparts.future.ObservableToFutureSupport
import com.netflix.hystrix.HystrixCommand
import rx.lang.scala.JavaConversions._

import scala.concurrent.Future

/**
 * Presents a way to wrap a normal Java Hystrix class so that it supports Scala Futures
 * @tparam T
 */
trait HystrixFutureSupport[T] extends ObservableToFutureSupport {
  this: HystrixCommand[T] =>

  /**
   * Returns a Future[T] based on the underlying HystrixCommand's #observe method
   * @return Future[T]
   */
  def future: Future[T] = observableToFuture(observe)

}
