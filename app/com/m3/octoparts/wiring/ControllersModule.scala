package com.m3.octoparts.wiring

import controllers.hystrix.HystrixController
import controllers.system.{ SystemConfigController, HealthcheckController, RandomMemcachedCacheKeysToCheck, SingleMemcachedCacheKeyToCheck }
import controllers.{ AdminController, CacheController, PartsController }
import presentation.NavbarLinks
import scala.concurrent.duration._
import com.softwaremill.macwire._

trait ControllersModule extends AggregatorServicesModule with HystrixModule {

  lazy val partsController = {
    val requestTimeout = typesafeConfig.getInt("timeouts.asyncRequestTimeout").millis
    val readClientCacheHeaders = {
      val disableFlag = typesafeConfig.getBoolean("read-client-cache.disabled")
      !disableFlag
    }
    new PartsController(partsService, configsRepository, requestTimeout, readClientCacheHeaders, zipkinService)
  }

  lazy val cacheController = wire[CacheController]

  lazy val adminController = wire[AdminController]

  lazy val navbarLinks = NavbarLinks(
    kibana = playConfig.getString("urls.kibana"),
    hystrixDashboard = playConfig.getString("urls.hystrixDashboard"),
    swaggerUI = playConfig.getString("urls.swaggerUI"),
    wiki = playConfig.getString("urls.wiki")
  )

  lazy val memcachedKeysToCheck = typesafeConfig.getInt("memcached.monitoring.randomChecks") match {
    case 0 => SingleMemcachedCacheKeyToCheck
    case n => RandomMemcachedCacheKeysToCheck(n)
  }

  lazy val healthcheckController = wire[HealthcheckController]

  lazy val systemConfigController = wire[SystemConfigController]

  lazy val hystrixController = new HystrixController()
}
