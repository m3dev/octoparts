package com.m3.octoparts.wiring.assembling

import com.m3.octoparts.wiring.ControllersModule
import com.softwaremill.macwire._
import controllers.{ Assets, AssetsComponents }
import play.api.ApplicationLoader.Context
import play.api.{ BuiltInComponents, BuiltInComponentsFromContext }
import play.api.i18n.I18nComponents
import play.api.libs.openid.OpenIDComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import router.Routes

object ApplicationComponents {

  /**
   * Type alias for what we consider some basic components that need to be in our app
   */
  type Essentials = BuiltInComponents with I18nComponents with OpenIDComponents with AhcWSComponents
}

class ApplicationComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with ControllersModule
    with EnvConfigLoader
    with I18nComponents
    with OpenIDComponents
    with AssetsComponents
    with AhcWSComponents {

  override lazy val configuration = withEnvConfig(context.initialConfiguration, environment)

  lazy val mode = context.environment.mode

  lazy val router: Router = {
    val prefix = "/"
    wire[Routes]
  }

  override lazy val httpFilters = Seq(zipkinTracingFilter)

}
