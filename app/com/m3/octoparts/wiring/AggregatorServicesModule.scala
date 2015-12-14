package com.m3.octoparts.wiring

import scala.concurrent.duration._
import com.m3.octoparts.aggregator.service.{ PartsService, PartResponseLocalContentSupport, PartRequestService }
import com.m3.octoparts.cache.PartResponseCachingSupport

trait AggregatorServicesModule extends RepositoriesModule with AggregatorHandlersModule with ExecutionContextsModule { module =>

  private implicit lazy val ec = partsServiceContext

  import scala.collection.JavaConverters._

  lazy val partRequestServiceBase = new PartRequestService(
    configsRepository,
    httpHandlerFactory,
    zipkinService,
    proxyConfig
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

  private[this] lazy val proxyConfig: Map[String, String] = {
    val config = configuration.getConfigSeq("proxies")
    val list = for {
      proxy <- config.getOrElse(Seq())
      id <- proxy.getString("id")
      url <- proxy.getString("url")
    } yield (id -> url)
    Map(list: _*)
  }
}