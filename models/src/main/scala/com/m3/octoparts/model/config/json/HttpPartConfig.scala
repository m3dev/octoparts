package com.m3.octoparts.model.config.json

import com.m3.octoparts.model.HttpMethod
import scala.concurrent.duration.Duration

/**
 * Created by Lloyd on 9/6/14.
 */
case class HttpPartConfig(partId: String,
                          owner: String,
                          uriToInterpolate: String,
                          description: String,
                          method: HttpMethod.Value,
                          hystrixConfig: HystrixConfig,
                          additionalValidStatuses: Set[Int] = Set.empty,
                          parameters: Set[PartParam] = Set.empty,
                          deprecatedInFavourOf: Option[String] = None,
                          cacheGroups: Set[CacheGroup] = Set.empty,
                          cacheTtl: Option[Duration] = Some(Duration.Zero), // in seconds
                          alertMailsEnabled: Boolean,
                          alertAbsoluteThreshold: Option[Int],
                          alertPercentThreshold: Option[Double],
                          alertInterval: Duration,
                          alertMailRecipients: Option[String])
