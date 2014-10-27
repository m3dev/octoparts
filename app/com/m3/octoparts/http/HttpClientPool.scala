package com.m3.octoparts.http

import com.m3.octoparts.util.KeyedResourcePool

import scala.util.Try

/**
 * A pool to manage HTTP clients.
 * Holds one HTTP client per partId.
 */
class HttpClientPool extends KeyedResourcePool[String, HttpClientLike] {

  def makeNew(key: String) = new InstrumentedHttpClient(key)

  def onRemove(value: HttpClientLike) = value match {
    case cl: AutoCloseable => Try {
      cl.close()
    }
  }

}