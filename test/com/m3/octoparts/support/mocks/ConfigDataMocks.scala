package com.m3.octoparts.support.mocks

import com.m3.octoparts.model.HttpMethod._
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
    method = Get,
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
    timeoutInMs = 50L,
    threadPoolConfig = Some(mockThreadConfig),
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

}
