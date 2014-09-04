package com.m3.octoparts.future

import rx.lang.scala.Observable

import scala.concurrent.Promise

/**
 * Trait for easy conversion between RX Observables and
 * Scala Futures
 *
 */
trait ObservableToFutureSupport {

  /**
   * Converts an Observable[A] to Future[A]
   *
   * Shouldn't really be used when the Observable is going to
   * give more than 1 result over the course of time.
   *
   * @param obs Observable[A]; can be Java or Scala variants
   * @tparam A
   * @return Future[A]
   */
  def observableToFuture[A](obs: Observable[A]) = {
    val promise = Promise[A]()
    obs.subscribe(
      onNext = x => promise.success(x),
      onError = e => promise.failure(e)
    )
    promise.future
  }

}