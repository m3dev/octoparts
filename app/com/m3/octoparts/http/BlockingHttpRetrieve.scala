package com.m3.octoparts.http

import java.net.URI

import com.m3.octoparts.model.HttpMethod
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity

/**
 * A trait to simplify retrieving the body of an Http Request
 */
trait BlockingHttpRetrieve {

  /*
   * members to be implemented in implementing traits/classes
   */
  def httpClient: HttpClientLike

  def method: HttpMethod.Value

  def maybeBody: Option[String]

  def uri: URI

  def headers: Seq[(String, String)]

  /**
   * The request that this class will pass to an HttpClient to execute
   *
   * If headers has been set in the instantiated class, then the request
   * will also have those headers set.
   *
   * @return HttpUriRequest
   */
  private[http] def request: HttpUriRequest = {
    import HttpMethod._
    val req = method match {
      case Get => new HttpGet(uri)
      case Head => new HttpHead(uri)
      case Delete => new HttpDelete(uri)
      case Options => new HttpOptions(uri)
      case Put => new HttpPut(uri)
      case Post => new HttpPost(uri)
      case Patch => new HttpPatch(uri)
      case x: HttpMethod => throw new IllegalArgumentException(s"Unsupported Http method $x")
    }
    req match {
      case heer: HttpEntityEnclosingRequest => maybeSetBody(heer)
      case _ =>
    }
    withHeaders(req)
  }

  /**
   * Does the actual retrieving
   */
  def retrieve(): HttpResponse = httpClient.retrieve(request)

  /**
   * Sets the body entity on the request
   */
  private def maybeSetBody(req: HttpEntityEnclosingRequest): Unit = {
    for (body <- maybeBody)
      req.setEntity(new StringEntity(body))
  }

  /**
   * Sets the headers on a HttpUriRequest
   * @param req HttpUriRequest
   * @return HttpUriRequest
   */
  private def withHeaders(req: HttpUriRequest): HttpUriRequest = {
    for ((name, value) <- headers) {
      req.setHeader(name, value)
    }
    req
  }
}