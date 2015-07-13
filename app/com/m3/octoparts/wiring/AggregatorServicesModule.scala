package com.m3.octoparts.wiring

import scala.concurrent.duration._
import com.m3.octoparts.aggregator.service.{ PartsService, PartResponseLocalContentSupport, PartRequestService }
import com.m3.octoparts.cache.PartResponseCachingSupport

import play.api.libs.concurrent.Akka
import play.api.Play.current

trait AggregatorServicesModule extends RepositoriesModule with AggregatorHandlersModule with ExecutionContextsModule { module =>

  private implicit lazy val ec = partsServiceContext

  lazy val partRequestServiceBase = new PartRequestService(
    configsRepository,
    httpHandlerFactory,
    zipkinService
  ) with PartResponseCachingSupport with PartResponseLocalContentSupport {
    val cacheOps = module.cacheOps
  }

  lazy val partsService = {
    new PartsService(
      partRequestServiceBase,
      partsReqLogger,
      typesafeConfig.getInt("timeouts.maximumAggregateReqTimeout").millis
    )
  }

}