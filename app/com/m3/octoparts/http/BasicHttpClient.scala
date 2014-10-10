package com.m3.octoparts.http

import java.io.Closeable
import java.nio.charset.{Charset, StandardCharsets}

import com.beachape.logging.LTSVLogger
import com.codahale.metrics.MetricRegistry.{name => registryName, _}
import com.codahale.metrics.httpclient._
import com.codahale.metrics.{Gauge, MetricRegistry}
import com.m3.octoparts.OctopartsMetricsRegistry
import com.m3.octoparts.util.TimingSupport
import org.apache.http.client.config.{CookieSpecs, RequestConfig}
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn._
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn._
import org.apache.http.pool.PoolStats
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
class BasicHttpClient(
  name: String,
  connectTimeout: Duration = 1.seconds,
  socketTimeout: Duration = 10.seconds,
  defaultEncoding: Charset = StandardCharsets.UTF_8)
    extends HttpClientLike
    with TimingSupport
    with Logging
    with Closeable {

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
      .setRequestExecutor(BasicHttpClient.InstrumentedRequestExecutor)
      .setConnectionManager(new InstrumentedHttpClientConnectionManager(OctopartsMetricsRegistry.default))
      .setDefaultRequestConfig(clientConfig)
      .build
  }

  // our custom HttpResponseHandler
  private val responseHandler = new HttpResponseHandler(defaultEncoding)

  /**
   * Givne a [[HttpUriRequest]], fetches the HttpResponse for it
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

  private[http] def registryName(key: String) = MetricRegistry.name(classOf[HttpClientConnectionManager], name, key)

  /**
   * A [[HttpClientConnectionManager]] which monitors the number of open connections.
   */
  private class InstrumentedHttpClientConnectionManager(metricsRegistry: MetricRegistry) extends PoolingHttpClientConnectionManager {
    BasicHttpClient.gauges.foreach {
      case (key, f) =>
        val gaugeName = registryName(key)
        try {
          metricsRegistry.register(gaugeName, new Gauge[Any] {
            def getValue = f(getTotalStats)
          })
        } catch {
          case e: IllegalArgumentException => LTSVLogger.warn(e, "Could not register gauge" -> gaugeName)
        }
    }


    override def shutdown() = {
      super.shutdown()
      BasicHttpClient.gauges.keys.foreach { key =>
        metricsRegistry.remove(registryName(key))
      }
    }
  }

  def close() = httpClient.close()
}

private[http] object BasicHttpClient {
  private val InstrumentedRequestExecutor = new InstrumentedHttpRequestExecutor(OctopartsMetricsRegistry.default, HttpClientMetricNameStrategies.QUERYLESS_URL_AND_METHOD)

  val gauges: Map[String, PoolStats => _] = Map(
    "available-connections" -> { ps: PoolStats => ps.getAvailable },
    "leased-connections" -> { ps: PoolStats => ps.getLeased },
    "max-connections" -> { ps: PoolStats => ps.getMax },
    "pending-connections" -> { ps: PoolStats => ps.getPending }
  )
}