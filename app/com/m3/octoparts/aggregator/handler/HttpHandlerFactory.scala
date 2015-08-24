package com.m3.octoparts.aggregator.handler

import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.model.config.HttpPartConfig

trait HttpHandlerFactory {

  /**
   * For tracing Http request times
   */
  implicit def zipkinService: ZipkinServiceLike

  /**
   * @param config a HttpCommandConfig entry
   * @return a handler ready to be used
   */
  def makeHandler(config: HttpPartConfig): Handler

}
