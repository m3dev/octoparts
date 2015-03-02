package com.m3.octoparts.aggregator.handler

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config.ShortPartParam
import com.twitter.zipkin.gen.Span

import scala.concurrent.Future

/**
 * A Handler simply maps a Map[Param, String] to a Future[PartResponse]
 * via it's #process method
 *
 * A Handler makes use of configuration data (partId, URI, registered params, etc.)
 * and request-specific data (PartRequestInfo) to form a request to the external dependency.
 * In most cases, this is done by parsing the PartRequestInfo
 * into a form that can be consumed by a generic HystrixCommand (see [[HttpPartRequestHandler]])
 */
trait Handler {

  type HandlerArguments = Map[ShortPartParam, Seq[String]]

  // Used primarily for creating a PartResponse, but also for logging purposes
  def partId: String

  def process(partRequestInfo: PartRequestInfo, arguments: HandlerArguments)(implicit parentSpan: Span): Future[PartResponse]

}
