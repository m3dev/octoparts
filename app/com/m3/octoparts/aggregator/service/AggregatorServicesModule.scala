package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.cache.PartResponseCachingSupport
import com.m3.octoparts.cache.client.CacheOps
import com.m3.octoparts.logging.PartRequestLogger
import com.m3.octoparts.repository.ConfigsRepository
import scaldi.Module

import scala.concurrent.duration._

class AggregatorServicesModule extends Module {

  implicit val glueContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  bind[PartRequestServiceBase] to new PartRequestService(
    inject[ConfigsRepository],
    inject[HttpHandlerFactory]
  ) with PartResponseCachingSupport {
    val cacheClient = inject[CacheOps]
  }

  bind[PartsService] to new PartsService(
    inject[PartRequestServiceBase],
    inject[PartRequestLogger],
    inject[Int](identified by "timeouts.maximumAggregateReqTimeout").millis
  )

}
