package com.m3.octoparts.http

import org.apache.http.client.methods.HttpUriRequest

/**
 * Basic interface for a synchronous HTTP retriever
 */
trait HttpClientLike {

  /**
   * Given a HttpUriRequest, does the fetch and returns a HttpResponse
   */
  def retrieve(request: HttpUriRequest): HttpResponse

}
