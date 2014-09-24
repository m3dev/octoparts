package com.m3.octoparts.aggregator.service

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.RejectedExecutionException

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.logging.{ LTSVLogWriter, LogUtil, PartRequestLogger }
import com.m3.octoparts.model.PartResponse
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.exception.HystrixRuntimeException.FailureType
import org.apache.http.conn.ConnectTimeoutException
import play.api.Logger
import skinny.util.LTSV

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

trait PartServiceErrorHandler extends LogUtil {
  implicit def executionContext: ExecutionContext

  def partRequestLogger: PartRequestLogger

  private def logRejection(partRequestInfo: PartRequestInfo, aReqTimeout: Duration, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogWriter.warn("Part" -> partId, "Execution rejected" -> message)
    partRequestLogger.logTimeout(partId, requestMeta.id, requestMeta.serviceId, aReqTimeout.toMillis)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logTimeout(partRequestInfo: PartRequestInfo, aReqTimeout: Duration, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogWriter.warn("Part" -> partId, "Timed out" -> aReqTimeout.toString)
    partRequestLogger.logTimeout(partId, requestMeta.id, requestMeta.serviceId, aReqTimeout.toMillis)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logInvalid(partRequestInfo: PartRequestInfo, duration: Duration, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogWriter.warn("Part" -> partId, "Invalid" -> message)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logShortCircuit(partRequestInfo: PartRequestInfo, duration: Duration, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogWriter.warn("Part" -> partId, "Hystrix" -> message)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logIOException(partRequestInfo: PartRequestInfo, duration: Duration, io: Throwable) = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogWriter.warn(io, "Part" -> partId)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(io.toString))
  }

  private def logOtherException(partRequestInfo: PartRequestInfo, duration: Duration, err: Throwable) = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogWriter.error(err, "Part" -> partId)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(err.toString))
  }

  protected def recoverChain(partRequestInfo: PartRequestInfo, aReqTimeout: Duration, f: Future[PartResponse]): Future[PartResponse] = {
    f.recoverWith {
      // unpile command exception
      case hre: HystrixRuntimeException if hre.getCause != null && hre.getCause != hre && hre.getFailureType == FailureType.COMMAND_EXCEPTION =>
        Future.failed(hre.getCause)

    }.recover {
      case IsTimeout(te) =>
        logTimeout(partRequestInfo, aReqTimeout, te.toString)
      case hre: HystrixRuntimeException if hre.getFailureType == FailureType.TIMEOUT =>
        logTimeout(partRequestInfo, aReqTimeout, hre.toString)

      case ree: RejectedExecutionException =>
        logRejection(partRequestInfo, aReqTimeout, ree.toString)
      case hre: HystrixRuntimeException if hre.getFailureType == FailureType.REJECTED_THREAD_EXECUTION
        || hre.getFailureType == FailureType.REJECTED_SEMAPHORE_EXECUTION
        || hre.getFailureType == FailureType.REJECTED_SEMAPHORE_FALLBACK =>
        logRejection(partRequestInfo, aReqTimeout, hre.toString)

      case hre: HystrixRuntimeException if hre.getFailureType == FailureType.SHORTCIRCUIT =>
        logShortCircuit(partRequestInfo, aReqTimeout, hre.toString)

      case iae: IllegalArgumentException =>
        logInvalid(partRequestInfo, aReqTimeout, iae.toString)

      case io: IOException =>
        logIOException(partRequestInfo, aReqTimeout, io)

      case err if NonFatal(err) =>
        logOtherException(partRequestInfo, aReqTimeout, err)
    }
  }

  private object IsTimeout {
    def apply(t: Throwable): Boolean = {
      t != null && (t match {
        case _: TimeoutException | _: ConnectTimeoutException | _: SocketTimeoutException => true
        case _ => false
      })
    }

    def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
  }

}
