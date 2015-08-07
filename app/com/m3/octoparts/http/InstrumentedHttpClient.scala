package com.m3.octoparts.http

import java.io.Closeable
import java.nio.charset.Charset

import com.beachape.logging.LTSVLogger
import com.codahale.metrics.{ Gauge, MetricRegistry }
import com.m3.octoparts.OctopartsMetricsRegistry
import com.m3.octoparts.model.config.HttpProxySettings
import org.apache.http._
import org.apache.http.client.HttpClient
import org.apache.http.client.config.{ CookieSpecs, RequestConfig }
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.pool.PoolStats
import org.apache.http.protocol.{ HttpContext, HttpRequestExecutor }
import com.m3.octoparts.BuildInfo
import org.apache.http.util.VersionInfo

import scala.concurrent.duration._

/**
 * Simple wrapper around Apache's HTTPClient for those times you
 * just want to hit a URI.
 *
 * Aims to be useful when you don't want to have HTTP requests
 * made in another thread/non-blocking, which most Scala libraries do.
 *
 * @param name is used to differentiate instances when printing statistics
 * @param connectTimeout max time waiting for the host to respond
 * @param socketTimeout max time spent receiving data from the host
 * @param defaultEncoding to be used when the response does not specify an encoding. set to a no-fail encoding like ASCII to handle binary data.
 */
class InstrumentedHttpClient(
  name: String,
  connectionPoolSize: Int,
  connectTimeout: FiniteDuration,
  socketTimeout: FiniteDuration,
  defaultEncoding: Charset,
  mbProxySettings: Option[HttpProxySettings])
    extends HttpClientLike
    with Closeable {
  import com.m3.octoparts.http.InstrumentedHttpClient._

  private[http] val connectionManager = new InstrumentedHttpClientConnectionMgr
  private val requestExecutor = new InstrumentedHttpRequestExecutor

  /**
   * Max time waiting for the connection pool to yield a connection
   */
  protected def connectionRequestTimeout: FiniteDuration = connectTimeout

  // the underlying Apache HTTP client
  private val httpClient = {

    val mbProxy = for {
      proxySettings <- mbProxySettings
    } yield {
      new HttpHost(proxySettings.host, proxySettings.port, proxySettings.scheme)
    }

    // Custom config that disables cookie handling so that we don't automatically
    // parse cookies into any shared cookie stores
    val clientConfig = RequestConfig.custom()
      .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
      .setConnectTimeout(connectTimeout.toMillis.toInt)
      .setConnectionRequestTimeout(connectionRequestTimeout.toMillis.toInt)
      .setSocketTimeout(socketTimeout.toMillis.toInt)
      .setProxy(mbProxy.orNull)
      .build()

    val apacheClientVersion = {
      val vi = Option(VersionInfo.loadVersionInfo("org.apache.http.client", classOf[HttpClientBuilder].getClassLoader))
      vi.map(_.getRelease).getOrElse(VersionInfo.UNAVAILABLE)
    }

    HttpClientBuilder
      .create
      .setRequestExecutor(requestExecutor)
      .setConnectionManager(connectionManager)
      .setDefaultRequestConfig(clientConfig)
      .setUserAgent(s"Apache-HttpClient ${apacheClientVersion} (Octoparts ${BuildInfo.version}, Scala ${BuildInfo.scalaVersion})")
      .build
  }

  // our custom HttpResponseHandler
  private val responseHandler = new HttpResponseHandler(defaultEncoding)

  /**
   * Given a [[HttpUriRequest]], fetches the HttpResponse for it
   * @param request HttpUriRequest
   * @return HttpResponse
   */
  def retrieve(request: HttpUriRequest): HttpResponse = httpClient.execute(request, responseHandler)

  /**
   * A [[PoolingHttpClientConnectionManager]] which monitors the number of open connections.
   */
  private[http] class InstrumentedHttpClientConnectionMgr extends PoolingHttpClientConnectionManager {
    // TODO use this after moving to httpclient-4.4
    // setValidateAfterInactivity(1) // always revalidate connections
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