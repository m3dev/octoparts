package controllers.hystrix

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

import com.netflix.config.DynamicPropertyFactory
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller
import play.api.libs.iteratee.Enumerator
import play.api.mvc._

import scala.concurrent.duration._

class HystrixController(
  defaultDelay: FiniteDuration = 1.second,
  pollerQueueSize: Int = 10000,
  defaultMaxClients: Int = 10)
    extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val maxClients = {
    DynamicPropertyFactory
      .getInstance()
      .getIntProperty("hystrix.stream.maxConcurrentConnections", defaultMaxClients)
  }

  private val numClients = new AtomicInteger(0)

  private val ChunkedHeaders = Seq(
    "Content-Type" -> s"text/event-stream; charset=${StandardCharsets.UTF_8}",
    "Cache-Control" -> Seq("no-cache", "no-store", "max-age=0", "must-revalidate").mkString(", "),
    "Pragma" -> "no-cache"
  )

  def stream(delayMs: Option[Int]) = Action {
    val numberConnections = numClients.incrementAndGet()
    val maxConnections = maxClients.get()
    if (numberConnections <= maxConnections) {
      val delay = delayMs.map(_.millis).getOrElse(defaultDelay)
      Ok.chunked(chunks(delay)).withHeaders(ChunkedHeaders: _*)
    } else {
      onDisconnect()
      ServiceUnavailable(s"MaxConcurrentConnections reached: $maxConnections")
    }
  }

  private def onDisconnect(): Unit = numClients.decrementAndGet()

  private def chunks(delay: FiniteDuration): Enumerator[Array[Byte]] = {
    val listener = new MetricsAsJsonPollerListener(pollerQueueSize)
    val poller = new HystrixMetricsPoller(listener, delay.toMillis.toInt)

    Enumerator.outputStream(new Streamer(poller, listener, delay).produce).onDoneEnumerating({
      onDisconnect()
      poller.shutdown()
    })
  }

}
