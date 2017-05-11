package com.m3.octoparts.wiring

import scala.concurrent.duration._
import com.m3.octoparts.aggregator.service.{ PartRequestService, PartResponseLocalContentSupport, PartsService }
import com.m3.octoparts.cache.PartResponseCachingSupport
import play.api.BuiltInComponents

trait AggregatorServicesModule
    extends RepositoriesModule
    with AggregatorHandlersModule
    with ExecutionContextsModule { module: BuiltInComponents =>

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
