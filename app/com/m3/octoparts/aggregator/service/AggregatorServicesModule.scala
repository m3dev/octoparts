package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.cache.{ CacheOps, PartResponseCachingSupport }
import com.m3.octoparts.logging.PartRequestLogger
import com.m3.octoparts.repository.ConfigsRepository
import play.api.libs.concurrent.Akka
import scaldi.Module

import scala.concurrent.duration._
import play.api.Play.current

class AggregatorServicesModule extends Module {

  implicit val partsServiceContext = Akka.system.dispatchers.lookup("contexts.parts-service")

  bind[PartRequestServiceBase] to new PartRequestService(
    inject[ConfigsRepository],
    inject[HttpHandlerFactory]
  ) with PartResponseCachingSupport with PartResponseLocalContentSupport {
    val cacheOps = inject[CacheOps]
  }

  bind[PartsService] to new PartsService(
    inject[PartRequestServiceBase],
    inject[PartRequestLogger],
    inject[Int](identified by "timeouts.maximumAggregateReqTimeout").millis
  )

}
