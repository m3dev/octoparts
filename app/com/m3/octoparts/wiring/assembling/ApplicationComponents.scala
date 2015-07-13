package com.m3.octoparts.wiring.assembling

import com.m3.octoparts.wiring.ControllersModule
import com.softwaremill.macwire._
import controllers.Assets
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.i18n.I18nComponents
import play.api.inject.{ NewInstanceInjector, SimpleInjector, Injector }
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.libs.ws.ning.NingWSComponents
import play.api.routing.Router

import router.Routes

class ApplicationComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with ControllersModule
    with I18nComponents
    with EnvConfigLoader
    with NingWSComponents {

  override lazy val configuration = withEnvConfig(context.initialConfiguration, context.environment.mode)

  lazy val mode = context.environment.mode

  lazy val assets: Assets = wire[Assets]

  lazy val router: Router = {
    val prefix = "/"
    wire[Routes]
  }

  override lazy val httpFilters = Seq(zipkinTracingFilter)

}
