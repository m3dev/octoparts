package com.m3.octoparts.aggregator.handler

import com.m3.octoparts.model.HttpMethod.Get
import com.m3.octoparts.http._
import com.m3.octoparts.hystrix._
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config._

import scala.concurrent.ExecutionContext

/**
 * Implementation of HttpPartRequestHandler in case class form
 *
 * @param uriToInterpolate String
 * @param httpMethod HttpMethod
 * @param registeredParams a Seq[Param] describing the r
 * @param hystrixExecutor HystrixExecutor
 */
class SimpleHttpPartRequestHandler(
  val partId: String,
  val httpClient: HttpClientLike,
  val uriToInterpolate: String,
  val httpMethod: HttpMethod.Value = Get,
  val additionalValidStatuses: Set[Int] = Set.empty,
  val registeredParams: Set[PartParam] = Set.empty,
  val hystrixExecutor: HystrixExecutor)(implicit val executionContext: ExecutionContext)
    extends HttpPartRequestHandler
