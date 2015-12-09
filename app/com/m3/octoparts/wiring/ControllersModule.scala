package com.m3.octoparts.wiring

import com.kenshoo.play.metrics.MetricsController
import com.m3.octoparts.wiring.assembling.ApplicationComponents
import controllers.hystrix.HystrixController
import controllers.system._
import controllers._
import pl.matisoft.swagger.ApiHelpController
import play.api.i18n.I18nComponents
import presentation.NavbarLinks
import scala.concurrent.duration._
import com.softwaremill.macwire._

trait ControllersModule
    extends AggregatorServicesModule
    with HystrixModule
    with AuthHandlerModule
    with I18nComponents
    with FiltersModule { this: ApplicationComponents.Essentials =>

  lazy val partsController = {
    val requestTimeout = typesafeConfig.getInt("timeouts.asyncRequestTimeout").millis
    val readClientCacheHeaders = {
      val disableFlag = typesafeConfig.getBoolean("read-client-cache.disabled")
      !disableFlag
    }
    wire[PartsController]
  }

  lazy val cacheController = wire[CacheController]

  lazy val adminController = {
    implicit val navbarLinks = NavbarLinks(
      kibana = configuration.getString("urls.kibana"),
      hystrixDashboard = configuration.getString("urls.hystrixDashboard"),
      swaggerUI = configuration.getString("urls.swaggerUI"),
      wiki = configuration.getString("urls.wiki")
    )
    wire[AdminController]
  }

  lazy val memcachedKeysToCheck = typesafeConfig.getInt("memcached.monitoring.randomChecks") match {
    case 0 => SingleMemcachedCacheKeyToCheck
    case n => RandomMemcachedCacheKeysToCheck(n)
  }

  lazy val healthcheckController = wire[HealthcheckController]

  lazy val systemConfigController = wire[SystemConfigController]

  lazy val hystrixController = new HystrixController()

  lazy val authController = wire[AuthController]

  lazy val apiHelpController = new ApiHelpController

  lazy val buildInfoController = new BuildInfoController

  lazy val defaultController = new Default

  lazy val metricsController = wire[MetricsController]
}
