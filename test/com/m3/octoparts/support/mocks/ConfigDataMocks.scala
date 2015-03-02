package com.m3.octoparts.support.mocks

import java.nio.charset.StandardCharsets

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.ParamType._
import com.m3.octoparts.model.config._
import org.joda.time.DateTime

import scala.concurrent.duration._

/**
 * Trait to allow us to get a hold of mock versions of our case classes
 */
trait ConfigDataMocks {

  val now = DateTime.now

  def mockPartParam = PartParam(
    id = Some(3L),
    required = true,
    versioned = false,
    paramType = Header,
    outputName = "userId",
    createdAt = now,
    updatedAt = now
  )

  def mockHttpPartConfig = HttpPartConfig(
    partId = "something",
    owner = "somebody",
    uriToInterpolate = "http://random.com",
    description = None,
    method = HttpMethod.Get,
    httpPoolSize = 5,
    httpConnectionTimeout = 1.second,
    httpSocketTimeout = 5.seconds,
    httpDefaultEncoding = StandardCharsets.US_ASCII,
    httpProxy = Some("localhost:666"),
    parameters = Set(mockPartParam),
    cacheTtl = Some(60.seconds),
    alertMailsEnabled = true,
    alertAbsoluteThreshold = Some(1000),
    alertPercentThreshold = Some(33),
    alertInterval = 10.minutes,
    alertMailRecipients = Some("l-chan@m3.com"),
    localContentsEnabled = true,
    localContents = Some("{}"),
    updatedAt = now,
    createdAt = now
  )

  def mockHystrixConfig = HystrixConfig(
    commandKey = "command",
    commandGroupKey = "GroupKey",
    timeout = 50.milliseconds,
    threadPoolConfig = Some(mockThreadConfig),
    localContentsAsFallback = false,
    createdAt = now,
    updatedAt = now
  )

  def mockThreadConfig = ThreadPoolConfig(
    id = Some(50L),
    threadPoolKey = "testThreadPool",
    createdAt = now,
    updatedAt = now
  )

  def mockCacheGroup = CacheGroup(
    owner = "mocked",
    name = "CacheMoney",
    createdAt = now,
    updatedAt = now
  )

  def mockPartRequestInfo = PartRequestInfo(
    requestMeta = mockRequestMeta,
    partRequest = mockPartRequest,
    noCache = false
  )

  def mockRequestMeta = RequestMeta(
    id = "id",
    serviceId = Some("serviceId"),
    userId = Some("uesrId"),
    sessionId = Some("sessionId"),
    requestUrl = Some("https://example.com/"),
    userAgent = Some("userAgent"),
    timeout = Some(30.seconds)
  )

  def mockPartRequest = PartRequest(
    partId = "partId",
    id = Some("id"),
    params = Nil
  )

  def mockPartResponse = PartResponse(
    partId = "pardId",
    id = "id",
    cookies = Nil,
    statusCode = Some(200),
    mimeType = Some("text/plain"),
    charset = Some("UTF-8"),
    cacheControl = CacheControl.NotSet,
    contents = Some("contents"),
    warnings = Seq("warning"),
    errors = Seq("errors"),
    retrievedFromCache = false,
    retrievedFromLocalContents = true
  )
}
