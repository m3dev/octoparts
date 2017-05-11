package com.m3.octoparts.wiring

import com.kenshoo.play.metrics.MetricsController
import com.m3.octoparts.util.ConfigMode
import com.m3.octoparts.wiring.assembling.ApplicationComponents
import controllers.hystrix.HystrixController
import controllers.system._
import controllers._
import pl.matisoft.swagger.ApiHelpController
import play.api.i18n.I18nComponents
import presentation.NavbarLinks

import scala.concurrent.duration._
import com.softwaremill.macwire._
import controllers.support.HttpPartConfigChecks
import play.api.mvc.ControllerComponents

trait ControllersModule
    extends AggregatorServicesModule
    with HystrixModule
    with AuthHandlerModule
    with I18nComponents
    with FiltersModule { this: ApplicationComponents.Essentials =>

  def controllerComponents: ControllerComponents

  implicit lazy val configMode: ConfigMode = ConfigMode(configuration.get[String]("application.env"))

  lazy val httpPartConfigChecks: HttpPartConfigChecks = new HttpPartConfigChecks(configMode)

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
      kibana = configuration.getOptional[String]("urls.kibana"),
      hystrixDashboard = configuration.getOptional[String]("urls.hystrixDashboard"),
      swaggerUI = configuration.getOptional[String]("urls.swaggerUI"),
      wiki = configuration.getOptional[String]("urls.wiki")
    )
    wire[AdminController]
  }

  lazy val memcachedKeysToCheck =
    typesafeConfig.getInt("memcached.monitoring.randomChecks") match {
      case 0 => SingleMemcachedCacheKeyToCheck
      case n => RandomMemcachedCacheKeysToCheck(n)
    }

  lazy val healthcheckController = wire[HealthcheckController]

  lazy val systemConfigController = wire[SystemConfigController]

  lazy val hystrixController =
    new HystrixController(actorSystem = actorSystem, controllerComponents)

  lazy val authController = wire[AuthController]

  lazy val apiHelpController = new ApiHelpController

  lazy val buildInfoController = wire[BuildInfoController]

  lazy val defaultController = new Default

  lazy val metricsController = wire[MetricsController]
}
