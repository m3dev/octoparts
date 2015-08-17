package com.m3.octoparts.aggregator.service

import com.beachape.logging.LTSVLogger
import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config.{ HttpPartConfig, ShortPartParam }
import com.m3.octoparts.repository.ConfigsRepository
import com.twitter.zipkin.gen.Span

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Base trait for PartRequestServices
 *
 * Implements the basic shared methods but leaves some methods to be implemented in
 * children classes / decorators
 */
trait PartRequestServiceBase extends RequestParamSupport {
  implicit def executionContext: ExecutionContext

  protected implicit def zipkinService: ZipkinServiceLike

  def repository: ConfigsRepository

  def handlerFactory: HttpHandlerFactory

  /**
   * The primary method responsible for processing a PartRequest into a Future[PartResponse]
   *
   * Does a look up in the repository for a dependency configuration that corresponds to the
   * partId inside the part request, parses out the relevant parameters out of the part request
   * according to the configuration data, and then passes that to a handler defined by the
   * configuration.
   *
   * @param pReq Part Request
   * @return Future[PartResponse]
   */
  def responseFor(pReq: PartRequestInfo)(implicit parentSpan: Span): Future[PartResponse] = {
    val fMaybeCi = repository.findConfigByPartId(pReq.partRequest.partId)
    fMaybeCi.flatMap {
      case Some(ci) => {
        val params = combineParams(ci.parameters, pReq)
        processWithConfig(ci, pReq, params)
      }
      case None => unsupported(pReq)
    }
  }

  /**
   * Returns a standard PartResponse for unsupported PartIds
   */
  private def unsupported(pReq: PartRequestInfo): Future[PartResponse] = {
    val partId = pReq.partRequest.partId
    LTSVLogger.warn("Request Id" -> pReq.requestMeta.id, "Requested PartId" -> partId, "Error" -> "not found")
    Future.successful(PartResponse(partId, pReq.partRequestId, errors = Seq(unsupportedMsg(partId))))
  }

  private[service] def unsupportedMsg(partId: String) = s"PartId $partId is unsupported"

  /**
   * In this base trait, simply instantiate a Handler for a given config and calls process
   * on it, passing in the parsed params. However, this method should be decorated with caching in
   * a stackable trait for things like caching.
   *
   * For example:
   *
   * {{{
   * // generateCacheDirective and fetchFromCacheOrElse need to be implemented elsewhere
   * trait CachingPartRequestService extends PartRequestService {
   * // Example decorator
   * abstract override def processWithConfig(ci: Config, params: Map[Param, String]): Future[Response] = {
   * val cacheDirective = generateCacheDirective(ci.cacheConfig, params)
   * fetchFromCacheOrElse(cacheDirective) {
   * super.processWithConfig(ci, params)
   * }
   * }
   * }
   * }}}
   *
   * @param ci Configuration data for a given dependency. Used for generating a handler factory in this base
   *           trait, but may be used for decorator purposes in Stackable traits.
   * @return Future[PartResponse], which includes adding deprecation notices
   */
  protected def processWithConfig(ci: HttpPartConfig, partRequestInfo: PartRequestInfo, params: Map[ShortPartParam, Seq[String]])(implicit parentSpan: Span): Future[PartResponse] = {
    val handler = handlerFactory.makeHandler(ci)
    val fResp = handler.process(partRequestInfo, params)
    fResp.map {
      resp =>
        val respWithId = resp.copy(id = partRequestInfo.partRequestId)
        handleDeprecation(ci, respWithId)
    }
  }

  private def handleDeprecation(ci: HttpPartConfig, resp: PartResponse): PartResponse = ci.deprecatedInFavourOf.collect {
    case newPartId if newPartId.length > 0 =>
      resp.copy(warnings = resp.warnings :+ deprecationMsg(ci.partId, newPartId))
  }.getOrElse(resp)

  private[service] def deprecationMsg(oldPartId: String, newPartId: String) = s"Please use $newPartId instead of $oldPartId"
}
