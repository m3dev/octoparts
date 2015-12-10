package com.m3.octoparts.wiring

import com.m3.octoparts.auth.OctopartsAuthHandler
import com.m3.octoparts.wiring.assembling.ApplicationComponents

trait AuthHandlerModule {
  this: ApplicationComponents.Essentials =>

  /**
   * To customise authentication handling in Admin, instantiate your own implementation of
   * [[OctopartsAuthHandler]] here. It will be injected into the Admin controller automatically
   */
  lazy val authHandler: Option[OctopartsAuthHandler] = None
}