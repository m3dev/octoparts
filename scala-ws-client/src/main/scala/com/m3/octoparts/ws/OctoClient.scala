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

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

/**
 * Default Octoparts [[OctoClientLike]] implementation
 *
 * Has a rescuer method that tries its best to recover from all reasonable errors.
 */
class OctoClient(val baseUrl: String, protected val httpRequestTimeout: Duration)(implicit val octoPlayApp: Application) extends OctoClientLike {

  protected def wsHolderFor(url: String) = WS.url(url).withRequestTimeout(httpRequestTimeout.toMillis.toInt)

  protected val rescuer: PartialFunction[Throwable, AggregateResponse] = {
    case JsResultException(e) => {
      logger.error(s"Octoparts service replied with invalid Json. Errors: $e")
      emptyReqResponse
    }
    case NonFatal(e) => {
      logger.error("Failed to get a valid response from Octoparts", e)
      emptyReqResponse
    }
  }
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
  protected def wsHolderFor(url: String): WSRequestHolder

  /**
   * PartialFunction for `recover`ing from errors when hitting Octoparts
   */
  protected def rescuer: PartialFunction[Throwable, AggregateResponse]

  /**
   * Simple named logger
   */
  protected val logger = Logger(this.getClass)

  // Url objects that map an Operation name to a Url
  protected[ws] case object Invoke extends NoPlaceholdersUrl { val url = endpointsApiBaseUrl(baseUrl) }
  protected[ws] case object InvalidateCache extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/part/%s" }
  protected[ws] case object InvalidateCacheFor extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/part/%s/%s/%s" }
  protected[ws] case object InvalidateCacheGroup extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/cache-group/%s" }
  protected[ws] case object InvalidateCacheGroupFor extends PlaceHoldersUrl { val url = s"${cacheApiBaseUrl(baseUrl)}/invalidate/cache-group/%s/params/%s" }

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
   */
  def invoke(aggReq: AggregateRequest)(implicit ec: ExecutionContext): Future[AggregateResponse] = {
    if (aggReq.requests.isEmpty)
      Future.successful(emptyReqResponse)
    else {
      val jsonBody = Json.toJson(aggReq)
      logger.debug(s"OctopartsId: ${aggReq.requestMeta.id}, RequestBody: $jsonBody")
      wsPost(urlFor(Invoke), jsonBody)
        .map(resp => resp.json.as[AggregateResponse])
        .recover(rescuer)
    }
  }

  /**
   *  Returns a Future[[com.m3.octoparts.model.AggregateResponse]] received from asynchronously invoking Octoparts using the
   *  provided argument object, and [[com.m3.octoparts.model.PartRequest]] list.
   *
   *  A [[RequestMetaBuilder]] type class instance for the first argument must be in scope at the call-site.
   */
  def invoke[A](obj: A, partReqs: Seq[PartRequest])(implicit reqMetaBuilder: RequestMetaBuilder[A], ec: ExecutionContext): Future[AggregateResponse] = {
    val reqMeta = reqMetaBuilder(obj)
    val aggReq = buildAggReq(reqMeta, partReqs)
    invoke(aggReq)
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
    wsPost(url, EmptyContent()).map { resp =>
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
   */
  protected def wsPost[A](url: String, body: A)(implicit wrt: Writeable[A], ct: ContentTypeOf[A]): Future[WSResponse] = wsHolderFor(url).post(body)

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