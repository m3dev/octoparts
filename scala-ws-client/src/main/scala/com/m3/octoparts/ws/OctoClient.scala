package com.m3.octoparts.ws

import com.google.common.net.UrlEscapers
import com.m3.octoparts.json.format.ReqResp._
import com.m3.octoparts.model._

import java.util.UUID
import play.api.http.{ ContentTypeOf, Writeable }
import play.api.mvc.Results.EmptyContent
import play.api.{ Application, Logger }
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import com.m3.octoparts.model.config.json.HttpPartConfig
import com.m3.octoparts.json.format.ConfigModel._ // For serdes of the models

/**
 * Default Octoparts [[OctoClientLike]] implementation
 *
 * Has a rescuer method that tries its best to recover from all reasonable errors.
 *
 * @param baseUrl The base URL of the Octoparts service you would like to hit with the instantiated client
 * @param clientTimeout The (HTTP) timeout that you would like this client to use. Note that sending [[AggregateRequest]]
 *                      will result in using the max of this parameter and the timeout on the request (if it exists)
 * @param extraWait Extra margin of wait time for timeouts. Defaults to 50 milliseconds.
 */
class OctoClient(val baseUrl: String, protected val clientTimeout: FiniteDuration, protected val extraWait: FiniteDuration = 50.milliseconds)(implicit val octoPlayApp: Application) extends OctoClientLike {

  protected def wsHolderFor(url: String, timeout: FiniteDuration) =
    WS.url(url).withRequestTimeout((timeout + extraWait).toMillis.toInt)

  protected def rescuer[A](defaultReturn: => A): PartialFunction[Throwable, A] = {
    case JsResultException(e) => {
      logger.error(s"Octoparts service replied with invalid Json. Errors: $e")
      defaultReturn
    }
    case NonFatal(e) => {
      logger.error("Failed to get a valid response from Octoparts", e)
      defaultReturn
    }
  }

  protected def rescueAggregateResponse: AggregateResponse = emptyReqResponse

  protected def rescueHttpPartConfigs: Seq[HttpPartConfig] = Seq.empty
}

/**
 * Octoparts client interface based on WS.
 *
 * Useful if you want to customise certain phases of building a WS request (e.g. logging, hooks)
 */
trait OctoClientLike {

  /**
   * Base URL for Octoparts
   */
  def baseUrl: String

  /**
   * Returns a [[play.api.libs.ws.WSRequestHolder]] for a given a URL string
   */
  protected def wsHolderFor(url: String, timeout: FiniteDuration): WSRequestHolder

  /**
   * The client-wide timeout
   */
  protected def clientTimeout: FiniteDuration

  /**
   * PartialFunction for `recover`ing from errors when hitting Octoparts
   */
  protected def rescuer[A](defaultReturn: => A): PartialFunction[Throwable, A]

  /**
   * Defines the [[AggregateResponse]] rescue return value
   */
  protected def rescueAggregateResponse: AggregateResponse

  /**
   * Defines the Seq[[HttpPartConfig]] rescue return value
   */
  protected def rescueHttpPartConfigs: Seq[HttpPartConfig]

  /**
   * Simple named logger
   */
  protected val logger = Logger(this.getClass)

  // Url objects that map an Operation name to a Url
  protected[ws] case object Invoke extends NoPlaceholdersUrl { val url = endpointsApiBaseUrl(baseUrl) }
  protected[ws] case object ListEndpoints extends NoPlaceholdersUrl { val url = s"${endpointsApiBaseUrl(baseUrl)}/list" }
  protected[ws] case object InvalidateCache extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/part/%s" }
  protected[ws] case object InvalidateCacheFor extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/part/%s/%s/%s" }
  protected[ws] case object InvalidateCacheGroup extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/cache-group/%s" }
  protected[ws] case object InvalidateCacheGroupFor extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/cache-group/%s/params/%s" }

  private val partIdFilterName: String = "partIdParams"

  /**
   * Given an [[ApiUrl]] and path segments, returns the full URL for that operation, filling in
   * path segments where appropriate
   */
  protected[ws] def urlFor(opUrl: ApiUrl, pathSegments: String*): String =
    if (opUrl.hasPlaceholders) {
      val escapedSegments = pathSegments.map(UrlEscapers.urlPathSegmentEscaper.escape)
      String.format(opUrl.url, escapedSegments: _*)
    } else opUrl.url

  /**
   * Returns a Future[[com.m3.octoparts.model.AggregateResponse]] received from asynchronously invoking Octoparts using
   * the provided [[com.m3.octoparts.model.AggregateRequest]]
   *
   * @param aggReq AggregateRequest
   * @param headers Optional set of headers that you can send with this request, defaults to none.
   */
  def invoke(aggReq: AggregateRequest, headers: (String, String)*)(implicit ec: ExecutionContext): Future[AggregateResponse] = {
    if (aggReq.requests.isEmpty)
      Future.successful(emptyReqResponse)
    else {
      val jsonBody = Json.toJson(aggReq)
      val timeout = aggReq.requestMeta.timeout.map(_ max clientTimeout).getOrElse(clientTimeout)
      logger.debug(s"OctopartsId: ${aggReq.requestMeta.id}, RequestBody: $jsonBody")
      wsPost(urlFor(Invoke), timeout, jsonBody, headers)
        .map(resp => resp.json.as[AggregateResponse])
        .recover(rescuer(rescueAggregateResponse))
    }
  }

  /**
   * Returns a Future[[com.m3.octoparts.model.AggregateResponse]] received from asynchronously invoking Octoparts using the
   * provided argument object, and [[com.m3.octoparts.model.PartRequest]] list.
   *
   * A [[RequestMetaBuilder]] type class instance for the first argument must be in scope at the call-site.
   *
   * @param obj Object of type A to build a request meta with
   * @param partReqs Part requests
   * @param headers Optional set of headers that you can send with this request, defaults to none.
   */
  def invoke[A](obj: A, partReqs: Seq[PartRequest], headers: (String, String)*)(implicit reqMetaBuilder: RequestMetaBuilder[A], ec: ExecutionContext): Future[AggregateResponse] = {
    val reqMeta = reqMetaBuilder(obj)
    val aggReq = buildAggReq(reqMeta, partReqs)
    invoke(aggReq, headers: _*)
  }

  /**
   * Returns a Future Seq[[com.m3.octoparts.model.config.json.HttpPartConfig]], which
   * describes all the endpoints registered to the Octoparts service.
   *
   * @param partIds a list of partIds in specific to retrieve endpoint info for.
   */
  def listEndpoints(partIds: String*)(implicit ec: ExecutionContext): Future[Seq[HttpPartConfig]] = {
    wsHolderFor(urlFor(ListEndpoints), clientTimeout)
      .withQueryString(partIds.map(n => partIdFilterName -> n): _*)
      .get()
      .map(resp => resp.json.as[Seq[HttpPartConfig]])
      .recover(rescuer(rescueHttpPartConfigs))
  }

  /**
   * Invalidates cache for a single part
   */
  def invalidateCache(partId: String)(implicit ec: ExecutionContext): Future[Boolean] =
    emptyPostOk(urlFor(InvalidateCache, partId))

  /**
   * Invalidates a region of the cache for a single part.
   * If the part does not exist, returns true anyways.
   */
  def invalidateCacheFor(partId: String, paramName: String, paramValue: String)(implicit ec: ExecutionContext): Future[Boolean] =
    emptyPostOk(urlFor(InvalidateCacheFor, partId, paramName, paramValue))

  /**
   * Invalidates a group of caches
   * If the group does not exist, returns false.
   */
  def invalidateCacheGroup(groupName: String)(implicit ec: ExecutionContext): Future[Boolean] =
    emptyPostOk(urlFor(InvalidateCacheGroup, groupName))

  /**
   * Invalidates a region for a group of caches
   * If the group does not exist, returns false.
   */
  def invalidateCacheGroupFor(groupName: String, paramValue: String)(implicit ec: ExecutionContext): Future[Boolean] =
    emptyPostOk(urlFor(InvalidateCacheGroupFor, groupName, paramValue))

  /**
   * Does a POST with empty content to the given URL and maps the response to true if the response status is
   * less than 400 or false otherwise
   */
  def emptyPostOk(url: String)(implicit ec: ExecutionContext): Future[Boolean] =
    wsPost(url, clientTimeout, EmptyContent()).map { resp =>
      val code = resp.status
      if (code < 400) {
        logger.trace(s"$url -> ${resp.body}")
        true
      } else if (code == 404) {
        logger.warn(s"404: ${resp.body}' for path $url")
        false
      } else {
        logger.warn(s"Unexpected response status: '${resp.body}' for path $url")
        false
      }
    }

  /**
   * Builds an [[com.m3.octoparts.model.AggregateRequest]] using [[com.m3.octoparts.model.RequestMeta]] and a list of [[com.m3.octoparts.model.PartRequest]]
   *
   * You may wish to (abstract) override this if you have you have your own requirements for
   * pulling shared data from [[com.m3.octoparts.model.RequestMeta]] into your [[com.m3.octoparts.model.PartRequest]]s
   */
  protected def buildAggReq(reqMeta: RequestMeta, partReqs: Seq[PartRequest]): AggregateRequest = AggregateRequest(reqMeta, partReqs)

  /**
   * Asynchronously sends a POST request to Octoparts.
   *
   * You may wish to (abstract) override this if you want to do custom error-handling on the WS request level.
   *
   * @param url URL to post to
   * @param timeout Timeout value for the request
   * @param headers headers to send along with the request
   */
  protected def wsPost[A](url: String, timeout: FiniteDuration, body: A, headers: Seq[(String, String)] = Seq.empty)(implicit wrt: Writeable[A], ct: ContentTypeOf[A]): Future[WSResponse] = {
    wsHolderFor(url, timeout)
      .withHeaders(headers: _*)
      .post(body)
  }

  /**
   * Generates a default dumb/empty [[com.m3.octoparts.model.AggregateResponse]].
   */
  protected def emptyReqResponse = AggregateResponse(ResponseMeta(id = UUID.randomUUID().toString, processTime = Duration.Zero), responses = Nil)

  /**
   * Drops the final forward slash from a string if it exists.
   *
   * As far as I can see, this does not use any Regexp..
   */
  protected def dropTrailingSlash(s: String) =
    if (s.endsWith("/"))
      s.substring(0, s.length - 1)
    else
      s

  /**
   * Returns a base URL for the Endpoint APIs
   */
  protected def endpointsApiBaseUrl(baseUrl: String): String = s"${dropTrailingSlash(baseUrl)}/octoparts/2"

  /**
   * Returns a base URL for the Cache-related APIs
   */
  protected def cacheApiBaseUrl(baseUrl: String): String = s"${endpointsApiBaseUrl(baseUrl)}/cache"

}

sealed trait ApiUrl {
  def url: String
  def hasPlaceholders: Boolean
}

sealed trait NoPlaceholdersUrl extends ApiUrl {
  val hasPlaceholders = false
}

sealed trait PlaceHoldersUrl extends ApiUrl {
  val hasPlaceholders = true
}