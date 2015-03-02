package com.m3.octoparts.aggregator.handler

import java.net.{ URI, URLEncoder }

import com.beachape.zipkin.TracedFuture
import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.http._
import com.m3.octoparts.hystrix._
import com.m3.octoparts.model.{ HttpMethod, PartResponse }
import com.m3.octoparts.model.config._
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.twitter.zipkin.gen.Span

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

/**
 * Trait describing a handler for processing of generic HTTP PartRequestInfo requests
 * that correspond to an external dependency
 */
trait HttpPartRequestHandler extends Handler {
  handler =>

  implicit def executionContext: ExecutionContext

  implicit def zipkinService: ZipkinServiceLike

  def httpClient: HttpClientLike

  def uriToInterpolate: String

  def httpMethod: HttpMethod.Value

  def additionalValidStatuses: Set[Int]

  def hystrixExecutor: HystrixExecutor

  /**
   * A regex for matching "${...}" placeholders in strings
   */
  private val PlaceholderReplacer: Regex = """\$\{([^\}]+)\}""".r

  // def registeredParams: Set[PartParam]

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
  def process(partRequestInfo: PartRequestInfo, hArgs: HandlerArguments)(implicit parentSpan: Span): Future[PartResponse] = {
    TracedFuture(s"Http request for - ${partRequestInfo.partRequest.partId}") { maybeSpan =>
      hystrixExecutor.future(
        createBlockingHttpRetrieve(partRequestInfo, hArgs, maybeSpan).retrieve(),
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
  }

  /**
   * Returns a BlockingHttpRetrieve command
   *
   * @param partRequestInfo the part request that spawned this HttpRetrieve
   * @param hArgs Handler arguments
   * @param tracingSpan [[Span]] generated for tracing; the details of this will be forwarded as headers
   * @return a command object that will perform an HTTP request on demand
   */
  def createBlockingHttpRetrieve(partRequestInfo: PartRequestInfo, hArgs: HandlerArguments, tracingSpan: Option[Span]): BlockingHttpRetrieve = {
    new BlockingHttpRetrieve {
      val httpClient = handler.httpClient
      def method = httpMethod
      val uri = new URI(buildUri(hArgs))
      val maybeBody = hArgs.collectFirst {
        case (p, values) if p.paramType == ParamType.Body && values.nonEmpty => values.head
      }
      val headers = {
        collectHeaders(hArgs) ++
          buildTracingHeaders(partRequestInfo) ++
          tracingSpan.fold(Map.empty[String, String])(zipkinService.spanToIdsMap)
      }
    }
  }

  private def buildTracingHeaders(partRequestInfo: PartRequestInfo): Seq[(String, String)] = {
    import HttpPartRequestHandler._
    Seq(
      AggregateRequestIdHeader -> partRequestInfo.requestMeta.id,
      PartRequestIdHeader -> partRequestInfo.partRequestId,
      PartIdHeader -> partRequestInfo.partRequest.partId
    )
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
   * Turns a string into an escaped string for cookies
   * @param c Cookie name or value
   * @return escaped cookie string
   */
  def escapeCookie(c: String) = URLEncoder.encode(c, "UTF-8")

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
    baseUri.addParams(kvs.toSeq)
  }

  /**
   * Replace all instances of "${...}" placeholders in the given string
   *
   * @param stringToInterpolate the string that includes placeholders
   * @param replacer a function that replaces the contents of the placeholder (excluding braces) with a string
   * @return the interpolated string
   */
  private def interpolate(stringToInterpolate: String)(replacer: String => String) =
    PlaceholderReplacer.replaceAllIn(stringToInterpolate, { m => replacer(m.group(1)) })

}

object HttpPartRequestHandler {
  val AggregateRequestIdHeader = "X-OCTOPARTS-PARENT-REQUEST-ID"
  val PartRequestIdHeader = "X-OCTOPARTS-REQUEST-ID"
  val PartIdHeader = "X-OCTOPARTS-PART-ID"
}