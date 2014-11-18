package com.m3.octoparts.http

import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.util.KeyedResourcePool

import scala.util.Try

/**
 * A pool to manage HTTP clients.
 * Holds one HTTP client per partId.
 */
class HttpClientPool extends KeyedResourcePool[String, HttpPartConfig, HttpClientLike] {

  def makeNew(partConfig: HttpPartConfig) = new InstrumentedHttpClient(
    partConfig.partId,
    partConfig.httpPoolSize,
    partConfig.httpConnectionTimeout,
    partConfig.httpSocketTimeout,
    partConfig.httpDefaultEncoding)

  def onRemove(value: HttpClientLike) = value match {
    case cl: AutoCloseable => Try {
      cl.close()
    }
  }

  protected def makeKey(obj: HttpPartConfig): String = obj.partId
}