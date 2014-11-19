package com.m3.octoparts.http

import java.io.Closeable
import java.nio.charset.{ Charset, StandardCharsets }

import com.beachape.logging.LTSVLogger
import com.codahale.metrics.{ Gauge, MetricRegistry }
import com.m3.octoparts.OctopartsMetricsRegistry
import com.m3.octoparts.util.TimingSupport
import org.apache.http.{ HttpClientConnection, HttpRequest }
import org.apache.http.client.HttpClient
import org.apache.http.client.config.{ CookieSpecs, RequestConfig }
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.pool.PoolStats
import org.apache.http.protocol.{ HttpContext, HttpRequestExecutor }
import skinny.logging.Logging
import skinny.util.LTSV

import scala.concurrent.duration._

/**
 * Simple wrapper around Apache's HTTPClient for those times you
 * just want to hit a URI.
 *
 * Aims to be useful when you don't want to have HTTP requests
 * made in another thread/non-blocking, which most Scala libraries do.
 *
 * @param name is used to differentiate instances when printing statistics
 */
class InstrumentedHttpClient(
  name: String,
  connectionPoolSize: Int = 20,
  connectTimeout: Duration = 1.seconds,
  socketTimeout: Duration = 10.seconds,
  defaultEncoding: Charset = StandardCharsets.UTF_8)
    extends HttpClientLike
    with TimingSupport
    with Logging
    with Closeable {
  import InstrumentedHttpClient._

  private[http] val connectionManager = new InstrumentedHttpClientConnectionMgr
  private val requestExecutor = new InstrumentedHttpRequestExecutor

  // the underlying Apache HTTP client
  private val httpClient = {
    // Custom config that disables cookie handling so that we don't automatically
    // parse cookies into any shared cookie stores
    val clientConfig = RequestConfig.custom()
      .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
      .setConnectTimeout(connectTimeout.toMillis.toInt)
      .setSocketTimeout(socketTimeout.toMillis.toInt)
      .build()

    HttpClientBuilder
      .create
      .setRequestExecutor(requestExecutor)
      .setConnectionManager(connectionManager)
      .setDefaultRequestConfig(clientConfig)
      .build
  }

  // our custom HttpResponseHandler
  private val responseHandler = new HttpResponseHandler(defaultEncoding)

  /**
   * Given a [[HttpUriRequest]], fetches the HttpResponse for it
   * @param request HttpUriRequest
   * @return HttpResponse
   */
  def retrieve(request: HttpUriRequest): HttpResponse = {
    debug(LTSV.dump("HTTP request" -> request.toString))
    time {
      httpClient.execute(request, responseHandler)
    } {
      (httpResponse, duration) =>
        debug(LTSV.dump(
          "HTTP status" -> httpResponse.status.toString,
          "HTTP response time" -> duration.toString
        ))
    }
  }

  /**
   * A [[PoolingHttpClientConnectionManager]] which monitors the number of open connections.
   */
  private[http] class InstrumentedHttpClientConnectionMgr extends PoolingHttpClientConnectionManager {
    setDefaultMaxPerRoute(connectionPoolSize)
    setMaxTotal(connectionPoolSize)

    private[http] def registryName(key: String) = MetricRegistry.name(classOf[HttpClientConnectionManager], name, key)

    gauges.foreach {
      case (key, f) =>
        val gaugeName = registryName(key)
        try {
          OctopartsMetricsRegistry.default.register(gaugeName, new Gauge[Int] {
            def getValue = f(getTotalStats)
          })
        } catch {
          case e: IllegalArgumentException => LTSVLogger.warn(e, "Could not register gauge" -> gaugeName)
        }
    }

    override def shutdown() = {
      super.shutdown()
      gauges.keys.foreach { key =>
        OctopartsMetricsRegistry.default.remove(registryName(key))
      }
    }
  }

  private[http] class InstrumentedHttpRequestExecutor extends HttpRequestExecutor with Closeable {

    private val registryName = MetricRegistry.name(classOf[HttpClient], name)

    override def execute(request: HttpRequest, conn: HttpClientConnection, context: HttpContext) = {
      val timerContext = OctopartsMetricsRegistry.default.timer(registryName).time
      try {
        super.execute(request, conn, context)
      } finally {
        timerContext.stop
      }
    }

    def close() = OctopartsMetricsRegistry.default.remove(registryName)
  }

  def close() = {
    try {
      httpClient.close()
    } finally {
      requestExecutor.close()
    }
  }
}

private[http] object InstrumentedHttpClient {

  val gauges: Map[String, PoolStats => Int] = Map(
    "available-connections" -> { ps: PoolStats => ps.getAvailable },
    "leased-connections" -> { ps: PoolStats => ps.getLeased },
    "max-connections" -> { ps: PoolStats => ps.getMax },
    "pending-connections" -> { ps: PoolStats => ps.getPending }
  )
}