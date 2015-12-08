package com.m3.octoparts.wiring.assembling

import com.m3.octoparts.wiring.ControllersModule
import com.softwaremill.macwire._
import controllers.Assets
import play.api.ApplicationLoader.Context
import play.api.{ BuiltInComponents, BuiltInComponentsFromContext }
import play.api.i18n.I18nComponents
import play.api.libs.openid.OpenIDComponents
import play.api.libs.ws.ning.NingWSComponents
import play.api.routing.Router

import router.Routes

object ApplicationComponents {

  /**
   * Type alias for what we consider some basic components that need to be in our app
   */
  type Essentials = BuiltInComponents with I18nComponents with OpenIDComponents with NingWSComponents
}

class ApplicationComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with ControllersModule
    with EnvConfigLoader
    with I18nComponents
    with OpenIDComponents
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
