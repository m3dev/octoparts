package com.m3.octoparts.http

import java.nio.charset.Charset

import com.kenshoo.play.metrics.Metrics
import com.m3.octoparts.http.HttpClientPool.HttpPartConfigClientKey
import com.m3.octoparts.model.config.{ HttpPartConfig, HttpProxySettings }
import com.m3.octoparts.util.KeyedResourcePool

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A pool to manage HTTP clients.
 * Holds one HTTP client per partId.
 */
class HttpClientPool(metrics: Metrics) extends KeyedResourcePool[HttpPartConfigClientKey, HttpClientLike] {

  protected def makeNew(key: HttpPartConfigClientKey) = new InstrumentedHttpClient(
    name = key.partId,
    connectionPoolSize = key.httpPoolSize,
    connectTimeout = key.httpConnectionTimeout,
    socketTimeout = key.httpSocketTimeout,
    defaultEncoding = key.httpDefaultEncoding,
    mbProxySettings = key.httpProxySettings,
    metrics = metrics
  )

  protected def onRemove(value: HttpClientLike) = value match {
    case cl: AutoCloseable => Try {
      cl.close()
    }
  }
}

object HttpClientPool {
  case class HttpPartConfigClientKey(partId: String, httpPoolSize: Int, httpConnectionTimeout: FiniteDuration, httpSocketTimeout: FiniteDuration, httpDefaultEncoding: Charset, httpProxySettings: Option[HttpProxySettings])

  object HttpPartConfigClientKey {
    def apply(part: HttpPartConfig): HttpPartConfigClientKey = HttpPartConfigClientKey(
      partId = part.partId,
      httpPoolSize = part.httpPoolSize,
      httpConnectionTimeout = part.httpConnectionTimeout,
      httpSocketTimeout = part.httpSocketTimeout,
      httpDefaultEncoding = part.httpDefaultEncoding.underlying,
      httpProxySettings = part.httpProxySettings
    )
  }
}