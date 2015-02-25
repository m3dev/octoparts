package com.m3.octoparts.aggregator.handler

import java.net.{ URI, URLEncoder }

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.http._
import com.m3.octoparts.hystrix._
import com.m3.octoparts.model.{ HttpMethod, PartResponse }
import com.m3.octoparts.model.config._
import com.netaporter.uri.Uri
import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.decoding.PercentDecoder
import com.netaporter.uri.encoding.PercentEncoder

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

/**
 * Trait describing a handler for processing of generic HTTP PartRequestInfo requests
 * that correspond to an external dependency
 */
trait HttpPartRequestHandler extends Handler {
  handler =>

  import HttpPartRequestHandler._

  implicit def executionContext: ExecutionContext

  def httpClient: HttpClientLike

  def uriToInterpolate: String

  def httpMethod: HttpMethod.Value

  def additionalValidStatuses: Set[Int]

  def hystrixExecutor: HystrixExecutor

  /**
   * Given arguments for this handler, builds a blocking HTTP request with the proper
   * URI, header and body params and sends it asynchronously in the context of
   * a Hystrix Command.
   *
   * Note that this Future should be used with care because it may
   * contain a Failure instead of Success. Make sure to transform with
   * .recover
   *
   * @param partRequestInfo info about the request, used for generating HTTP headers for request tracing
   * @param hArgs Preparsed HystrixArguments
   * @return Future[PartResponse]
   */
  def process(partRequestInfo: PartRequestInfo, hArgs: HandlerArguments): Future[PartResponse] = {
    hystrixExecutor.future(
      createBlockingHttpRetrieve(partRequestInfo, hArgs).retrieve().copy(),
      maybeContents => HttpResponse(
        status = 203, // 203 -> Non-authoritative info
        body = maybeContents,
        fromFallback = true,
        message = "From fallback"
      )
    ).map {
        createPartResponse
      }
  }

  /**
   * Returns a BlockingHttpRetrieve command
   *
   * @param hArgs Handler arguments
   * @return a command object that will perform an HTTP request on demand
   */
  def createBlockingHttpRetrieve(partRequestInfo: PartRequestInfo, hArgs: HandlerArguments): BlockingHttpRetrieve = {
    new BlockingHttpRetrieve {
      val httpClient = handler.httpClient
      def method = httpMethod
      val uri = new URI(buildUri(hArgs).toString(VeryConservativeUriConfig))
      val maybeBody = hArgs.collectFirst {
        case (p, values) if p.paramType == ParamType.Body && values.nonEmpty => values.head
      }
      val headers = collectHeaders(hArgs) ++ buildTracingHeaders(partRequestInfo)
    }
  }

  /**
   * Transforms a HttpResponse case class into a PartResponse
   * @param httpResp HttpResponse
   * @return PartREsponse
   */
  def createPartResponse(httpResp: HttpResponse) = PartResponse(
    partId,
    id = partId,
    cookies = httpResp.cookies,
    statusCode = Some(httpResp.status),
    mimeType = httpResp.mimeType,
    charset = httpResp.charset,
    cacheControl = httpResp.cacheControl,
    contents = httpResp.body,
    retrievedFromLocalContents = httpResp.fromFallback,
    errors = if (httpResp.status < 400 || additionalValidStatuses.contains(httpResp.status)) Nil else Seq(httpResp.message)
  )

  /**
   * Isolates the header-related arguments, taking care to escape Cookie headers
   * @param hArgs arguments
   * @return Map[String, String]
   */
  def collectHeaders(hArgs: HandlerArguments): Seq[(String, String)] = {
    // group Cookies. According to RFC 6265, at most one Cookie header may be sent.
    val cookieHeadersElements = for {
      (p, values) <- hArgs if p.paramType == ParamType.Cookie
      cookieName = escapeCookie(p.outputName)
      v <- values
    } yield {
      s"$cookieName=${escapeCookie(v)}"
    }
    val cookieHeaderValue = if (cookieHeadersElements.isEmpty) None else Some(cookieHeadersElements.mkString("; "))

    // for other headers, no grouping is done. Note: duplicate headers are allowed!
    cookieHeaderValue.map("Cookie" -> _).toSeq ++ (for {
      (p, values) <- hArgs if p.paramType == ParamType.Header if p.outputName != "Cookie"
      v <- values
    } yield {
      p.outputName -> v
    })
  }

  /**
   * Takes a "base" URI string in the format of "http://example.com/${hello}" and returns an interpolated
   * string
   *
   * @param hArgs HttpArguments
   * @return Uri
   */
  private[handler] def buildUri(hArgs: HandlerArguments): Uri = {
    val baseUri = interpolate(uriToInterpolate) { key =>
      val ThePathParam = ShortPartParam(key, ParamType.Path)
      val maybeParamsVal: Option[String] = hArgs.collectFirst {
        case (ThePathParam, v) if v.nonEmpty => v.head
      }
      maybeParamsVal.getOrElse("")
    }
    val kvs = for {
      // toSeq because we don't want the result to be a map (with unique keys)
      (p, values) <- hArgs.toSeq if p.paramType == ParamType.Query
      v <- values
    } yield p.outputName -> v
    Uri.parse(baseUri).addParams(kvs.toSeq)
  }
}

object HttpPartRequestHandler {
  val AggregateRequestIdHeader = "X-OCTOPARTS-PARENT-REQUEST-ID"
  val PartRequestIdHeader = "X-OCTOPARTS-REQUEST-ID"
  val PartIdHeader = "X-OCTOPARTS-PART-ID"

  /**
   * Required because for some reason java.net.URI does not accept double quotes in the query string
   */
  private val VeryConservativeUriConfig = UriConfig(PercentEncoder(PercentEncoder.DEFAULT_CHARS_TO_ENCODE + '"'), PercentDecoder)

  /**
   * A regex for matching "${...}" placeholders in strings
   */
  private val PlaceholderReplacer: Regex = """\$\{([^\}]+)\}""".r

  /**
   * Replace all instances of "${...}" placeholders in the given string
   *
   * @param stringToInterpolate the string that includes placeholders
   * @param replacer a function that replaces the contents of the placeholder (excluding braces) with a string
   * @return the interpolated string
   */
  private def interpolate(stringToInterpolate: String)(replacer: String => String) =
    PlaceholderReplacer.replaceAllIn(stringToInterpolate, { m => replacer(m.group(1)) })

  /**
   * Turns a string into an escaped string for cookies
   * @param c Cookie name or value
   * @return escaped cookie string
   */
  private def escapeCookie(c: String) = URLEncoder.encode(c, "UTF-8")

  private def buildTracingHeaders(partRequestInfo: PartRequestInfo): Seq[(String, String)] = {
    Seq(
      AggregateRequestIdHeader -> partRequestInfo.requestMeta.id,
      PartRequestIdHeader -> partRequestInfo.partRequestId,
      PartIdHeader -> partRequestInfo.partRequest.partId
    )
  }
}