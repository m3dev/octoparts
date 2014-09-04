package com.m3.octoparts.aggregator.handler

import java.net.{ URI, URLEncoder }

import com.m3.octoparts.http.{ HttpResponse, _ }
import com.m3.octoparts.hystrix._
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config._
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

/**
 * Trait describing a handler for processing of generic HTTP PartRequestInfo requests
 * that correspond to an external dependency
 */
trait HttpPartRequestHandler extends Handler {
  handler =>

  implicit def executionContext: ExecutionContext

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
   * @param hArgs Preparsed HystrixArguments
   * @return Future[PartResponse]
   */
  def process(hArgs: HandlerArguments): Future[PartResponse] = {
    hystrixExecutor.future {
      createBlockingHttpRetrieve(hArgs).retrieve()
    }.map {
      createPartResponse
    }
  }

  /**
   * Returns a BlockingHttpRetrieve command
   *
   * @param hArgs Handler arguments
   * @return a command object that will perform an HTTP request on demand
   */
  def createBlockingHttpRetrieve(hArgs: HandlerArguments): BlockingHttpRetrieve = {
    new BlockingHttpRetrieve {
      val httpClient = handler.httpClient
      val method = httpMethod
      val uri = new URI(buildUri(hArgs))
      val maybeBody = hArgs.collectFirst {
        case (p, v) if p.paramType == ParamType.Body => v
      }
      val headers = collectHeaders(hArgs)
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
    hArgs.toSeq.collect {
      case (p, v) if p.paramType == ParamType.Header => p.outputName -> v
      case (p, v) if p.paramType == ParamType.Cookie => "Cookie" -> (escapeCookie(p.outputName) + "=" + escapeCookie(v))
    }
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
      val maybeParamsVal: Option[String] = hArgs.collectFirst {
        case (p, v) if p.paramType == ParamType.Path && p.outputName == key => v
      }
      maybeParamsVal.getOrElse("")
    }
    baseUri.addParams(hArgs.collect { case (p, v) if p.paramType == ParamType.Query => (p.outputName, v) }.toSeq)
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