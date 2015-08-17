package com.m3.octoparts.aggregator.service

import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.repository.ConfigsRepository

import scala.concurrent.ExecutionContext

/**
 * Undecorated implementation of PartRequestServiceBase
 */
class PartRequestService(
  val repository: ConfigsRepository,
  val handlerFactory: HttpHandlerFactory,
  implicit val zipkinService: ZipkinServiceLike)(implicit val executionContext: ExecutionContext)
    extends PartRequestServiceBase
