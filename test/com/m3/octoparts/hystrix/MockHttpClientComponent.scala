package com.m3.octoparts.hystrix

import org.apache.http.client.methods.HttpUriRequest
import com.m3.octoparts.http.{ HttpResponse, HttpClientLike }

/**
 * Mock HTTP requests
 */
trait MockHttpClientComponent {
  val httpClient = new HttpClientLike {
    def retrieve(request: HttpUriRequest): HttpResponse = HttpResponse(200, "OK", body = Some("<h2>Hello</h2>"))
  }
}

object MockHttpClientComponent extends MockHttpClientComponent {
}
