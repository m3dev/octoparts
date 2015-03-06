package com.m3.octoparts.client

import java.io.Closeable
import java.util.UUID
import javax.annotation.{ Nonnull, Nullable }

import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import com.google.common.net.UrlEscapers
import com.m3.octoparts.model.config.json.HttpPartConfig
import com.m3.octoparts.model.{ AggregateRequest, RequestMeta }
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, ListenableFuture }
import org.slf4j.LoggerFactory

import scala.annotation.varargs
import scala.concurrent.duration._

private[client] object OctopartsApiBuilder {
  def formatWithUriEscape(format: String, args: String*): String = {
    val escapedArgs = for (arg <- args) yield {
      Option(arg).fold("null")(UrlEscapers.urlPathSegmentEscaper.escape)
    }
    format.format(escapedArgs: _*)
  }

  private val Log = LoggerFactory.getLogger(classOf[OctopartsApiBuilder])
  private[client] val Mapper = new ObjectMapper
  Mapper.registerModule(ExtendedScalaModule)
  // future-proofing
  Mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

class OctopartsApiBuilder(@Nonnull apiRootUrl: String, @Nullable serviceId: String, @Nonnull asyncHttpClientConfig: AsyncHttpClientConfig) extends Closeable {

  import com.m3.octoparts.client.OctopartsApiBuilder._

  private val octopartsApiEndpointUrl = s"$apiRootUrl/octoparts/2"
  private val cacheApiEndpointUrl = s"$octopartsApiEndpointUrl/cache"
  private val asyncHttpClient = new AsyncHttpClient(asyncHttpClientConfig)

  def this(@Nonnull apiRootUrl: String, @Nullable serviceId: String) = this(apiRootUrl, serviceId,
    new AsyncHttpClientConfig.Builder().setCompressionEnforced(true).setFollowRedirect(false).setAllowPoolingConnections(true).setMaxRequestRetry(0).build
  )

  def close() = asyncHttpClient.close()

  /**
   *
   * @param userId optional user id
   * @param sessionId optional session id
   * @param userAgent optional user agent
   * @param requestUrl optional request URL
   * @param timeoutMs This value is enforced in the octoparts server.
   */
  def newRequest(@Nullable userId: String, @Nullable sessionId: String, @Nullable userAgent: String, @Nullable requestUrl: String, @Nullable timeoutMs: java.lang.Long): RequestBuilder = {
    val timeoutOpt = if (timeoutMs == null) None else Some(timeoutMs.longValue().millis)
    val requestMeta = RequestMeta(UUID.randomUUID.toString, Option(serviceId), Option(userId), Option(sessionId), Option(requestUrl), Option(userAgent), timeoutOpt)
    new RequestBuilder(requestMeta)
  }

  private[client] def toHttp(aggregateRequest: AggregateRequest, additionalHeaders: (String, String)*) = {
    Log.debug(s"${aggregateRequest.requests.size} octoparts")
    val jsonContent = Mapper.writeValueAsBytes(aggregateRequest)
    var requestBuilder = asyncHttpClient.
      preparePost(octopartsApiEndpointUrl).
      addHeader("Content-Type", "application/json;charset=UTF-8").
      addHeader("Content-Length", jsonContent.length.toString).
      setBody(jsonContent)
    additionalHeaders.foreach {
      case (k, v) => requestBuilder = requestBuilder.addHeader(k, v)
    }
    requestBuilder.build
  }

  /**
   * Sends a request to the Octoparts server.
   */
  def submit(@Nonnull aggregateRequest: AggregateRequest, additionalHeaders: (String, String)*): ListenableFuture[ResponseWrapper] = {
    asyncHttpClient.executeRequest(toHttp(aggregateRequest, additionalHeaders: _*), new AggregateResponseExtractor(aggregateRequest))
  }

  /**
   * Invalidates the whole cache for a part.
   * If the part does not exist, returns true anyways.
   */
  @Nonnull def invalidateCache(@Nonnull partId: String): ListenableFuture[java.lang.Boolean] = {
    val uri = formatWithUriEscape("/invalidate/part/%s", partId)
    sendCachePost(uri)
  }

  /**
   * Invalidates a region of the cache for a part.
   * If the part or the region does not exist, returns true anyways.
   */
  @Nonnull def invalidateCacheFor(@Nonnull partId: String, @Nonnull parameterName: String, @Nullable parameterValue: String): ListenableFuture[java.lang.Boolean] = {
    val uri = formatWithUriEscape("/invalidate/part/%s/%s/%s", partId, parameterName, parameterValue)
    sendCachePost(uri)
  }

  /**
   * Invalidates a group of caches
   * If the group does not exist, returns false.
   */
  @Nonnull def invalidateCacheGroup(@Nonnull groupName: String): ListenableFuture[java.lang.Boolean] = {
    val uri = formatWithUriEscape("/invalidate/cacheGroup/%s", groupName)
    sendCachePost(uri)
  }

  /**
   * Invalidates a region for a group of caches
   * If the group does not exist, returns false.
   */
  @Nonnull def invalidateCacheGroupFor(@Nonnull groupName: String, @Nullable parameterValue: String): ListenableFuture[java.lang.Boolean] = {
    val uri = formatWithUriEscape("/invalidate/cacheGroup/%s/params/%s", groupName, parameterValue)
    sendCachePost(uri)
  }

  /**
   * @param partIds when specified, filters the endpoint list. Note: not specifying a filter will prevent Octoparts from using the part config cache.
   */
  @varargs @Nonnull def listEndpoints(@Nonnull partIds: String*): ListenableFuture[java.util.List[HttpPartConfig]] = {
    import scala.collection.convert.Wrappers._
    val queryParams: java.util.Map[String, java.util.List[String]] = {
      if (partIds.isEmpty) java.util.Collections.emptyMap() else MutableMapWrapper(scala.collection.mutable.Map("partIdParams" -> SeqWrapper(partIds)))
    }
    val request = asyncHttpClient.
      prepareGet(s"$octopartsApiEndpointUrl/list").
      setQueryParams(queryParams).
      build
    asyncHttpClient.executeRequest(request, EndpointListExtractor)
  }

  @Nonnull private def sendCachePost(@Nonnull uri: String): ListenableFuture[java.lang.Boolean] = {
    val request = asyncHttpClient.preparePost(cacheApiEndpointUrl + uri).build
    asyncHttpClient.executeRequest(request, new OKResponseExtractor(uri))
  }

}