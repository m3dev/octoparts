package com.m3.octoparts.wiring

import com.m3.octoparts.aggregator.handler.SimpleHttpHandlerFactory
import com.softwaremill.macwire._

trait AggregatorHandlersModule extends HttpClientPoolModule {

  private implicit val glueContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  lazy val httpHandlerFactory = wire[SimpleHttpHandlerFactory]

}