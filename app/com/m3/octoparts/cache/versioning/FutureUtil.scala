package com.m3.octoparts.cache.versioning

import scala.concurrent.{ ExecutionContext, Future }

private[versioning] object FutureUtil {

  def thenDoIfWasNone(futureOpt: Future[Option[_]])(
    action: => Future[Unit]
  )(implicit executionContext: ExecutionContext): Future[Boolean] = {
    futureOpt.flatMap {
      _.fold {
        val result: Future[Unit] = action
        result.map { unit => true }
      } {
        some => Future.successful[Boolean](false)
      }
    }
  }

  def doIfWasSome[R](futureOpt: Future[Option[R]])(
    action: R => Unit
  )(implicit executionContext: ExecutionContext): Future[Unit] = {
    futureOpt.map(_.fold {}(action))
  }

}
