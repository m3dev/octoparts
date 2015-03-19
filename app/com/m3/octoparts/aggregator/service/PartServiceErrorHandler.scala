package com.m3.octoparts.aggregator.service

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.RejectedExecutionException

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.logging.{ LogUtil, PartRequestLogger }
import com.beachape.logging.LTSVLogger
import com.m3.octoparts.model.PartResponse
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.exception.HystrixRuntimeException.FailureType
import org.apache.http.conn.ConnectTimeoutException

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
    LTSVLogger.warn("Part" -> partId, "Execution rejected" -> message)
    partRequestLogger.logTimeout(partId, requestMeta.id, requestMeta.serviceId, aReqTimeout.toMillis)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logTimeout(partRequestInfo: PartRequestInfo, aReqTimeout: Duration, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogger.warn("Part" -> partId, "Timed out" -> aReqTimeout.toString)
    partRequestLogger.logTimeout(partId, requestMeta.id, requestMeta.serviceId, aReqTimeout.toMillis)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logInvalid(partRequestInfo: PartRequestInfo, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogger.warn("Part" -> partId, "Invalid" -> message)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logShortCircuit(partRequestInfo: PartRequestInfo, message: String): PartResponse = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogger.warn("Part" -> partId, "Hystrix" -> message)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(message))
  }

  private def logIOException(partRequestInfo: PartRequestInfo, io: Throwable) = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogger.warn(io, "Part" -> partId)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(io.toString))
  }

  private def logOtherException(partRequestInfo: PartRequestInfo, err: Throwable) = {
    val partId = partRequestInfo.partRequest.partId
    val requestMeta = partRequestInfo.requestMeta
    LTSVLogger.error(err, "Part" -> partId)
    partRequestLogger.logFailure(partId, requestMeta.id, requestMeta.serviceId, statusCode = None)
    PartResponse(partId, partRequestInfo.partRequestId, errors = Seq(err.toString))
  }

  protected def recoverChain(partRequestInfo: PartRequestInfo, aReqTimeout: Duration, f: Future[PartResponse]): Future[PartResponse] = {
    f.recoverWith {
      // unpile command exception
      case hre: HystrixRuntimeException if Option(hre.getCause).exists(_ != hre) && hre.getFailureType == FailureType.COMMAND_EXCEPTION =>
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
        logShortCircuit(partRequestInfo, hre.toString)

      case iae: IllegalArgumentException =>
        logInvalid(partRequestInfo, iae.toString)

      case io: IOException =>
        logIOException(partRequestInfo, io)

      case err if NonFatal(err) =>
        logOtherException(partRequestInfo, err)
    }
  }

  private object IsTimeout {
    def unapply(t: Throwable): Option[Throwable] = t match {
      case _: TimeoutException | _: ConnectTimeoutException | _: SocketTimeoutException => Some(t)
      case _ => None
    }
  }

}
