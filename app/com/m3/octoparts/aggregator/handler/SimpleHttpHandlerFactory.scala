package com.m3.octoparts.aggregator.handler

import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.http.HttpClientPool
import com.m3.octoparts.hystrix.HystrixExecutor
import com.m3.octoparts.model.config.HttpPartConfig

import scala.concurrent.ExecutionContext

class SimpleHttpHandlerFactory(httpClientPool: HttpClientPool, implicit val zipkinService: ZipkinServiceLike)(
    implicit executionContext: ExecutionContext) extends HttpHandlerFactory {

  override def makeHandler(config: HttpPartConfig) = {
    // Get or create the HTTP client corresponding to this partId
    val httpClient = httpClientPool.getOrCreate(config.partId)

    new SimpleHttpPartRequestHandler(
      config.partId,
      httpClient,
      config.uriToInterpolate,
      config.method,
      config.additionalValidStatuses,
      config.parameters,
      HystrixExecutor(config)
    )
  }

}