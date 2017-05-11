package com.m3.octoparts.wiring

import com.m3.octoparts.aggregator.handler.SimpleHttpHandlerFactory
import com.softwaremill.macwire._
import play.api.BuiltInComponents

trait AggregatorHandlersModule extends HttpClientPoolModule { this: BuiltInComponents =>

  private lazy implicit val glueContext = actorSystem.dispatcher

  lazy val httpHandlerFactory = wire[SimpleHttpHandlerFactory]

}