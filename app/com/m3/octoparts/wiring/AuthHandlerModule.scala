package com.m3.octoparts.wiring

import com.m3.octoparts.auth.OctopartsAuthHandler
import play.api.BuiltInComponents
import play.api.i18n.I18nComponents
import play.api.libs.openid.OpenIDComponents
import play.api.libs.ws.ning.NingWSComponents

trait AuthHandlerModule {
  // TODO: replace this with ApplicationComponents.Essentials once https://github.com/adamw/macwire/pull/87 is merged
  this: BuiltInComponents with I18nComponents with OpenIDComponents with NingWSComponents =>

  /**
   * To customise authentication handling in Admin, instantiate your own implementation of
   * [[OctopartsAuthHandler]] here. It will be injected into the Admin controller automatically
   */
  lazy val authHandler: Option[OctopartsAuthHandler] = None
}