package com.m3.octoparts.wiring

import play.api.Application

/**
 * Module that contains everything
 *
 * Useful for overriding certain members as required
 */
class Amalgamated(appFactory: => Application) extends ControllersModule {

  lazy val app = appFactory

}