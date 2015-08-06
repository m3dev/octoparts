package com.m3.octoparts.aggregator.handler

import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.http._
import com.m3.octoparts.hystrix._
import com.m3.octoparts.model.HttpMethod

import scala.collection.SortedSet
import scala.concurrent.ExecutionContext

/**
 * Implementation of HttpPartRequestHandler in case class form
 *
 * @param uriToInterpolate String
 * @param httpMethod HttpMethod
 * @param hystrixExecutor HystrixExecutor
 */
class SimpleHttpPartRequestHandler(
  val partId:                  String,
  val httpClient:              HttpClientLike,
  val uriToInterpolate:        String,
  val httpMethod:              HttpMethod.Value,
  val additionalValidStatuses: SortedSet[Int],
  val hystrixExecutor:         HystrixExecutor
)(implicit val executionContext: ExecutionContext, implicit val zipkinService: ZipkinServiceLike)
    extends HttpPartRequestHandler
