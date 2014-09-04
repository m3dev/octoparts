package com.m3.octoparts.aggregator.handler

import com.m3.octoparts.http.HttpClientPool
import scaldi.Module

class AggregatorHandlersModule extends Module {

  implicit val glueContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  bind[HttpHandlerFactory] to new SimpleHttpHandlerFactory(inject[HttpClientPool])

}
