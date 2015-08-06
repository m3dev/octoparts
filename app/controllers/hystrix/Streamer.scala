package controllers.hystrix

import java.io.OutputStream
import java.nio.charset.StandardCharsets

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

private class Streamer(
    poller: HystrixMetricsPoller, listener: MetricsAsJsonPollerListener, delay: FiniteDuration
)(
    implicit
    executionContext: ExecutionContext
) {

  import play.api.Play.current
  import play.api.libs.concurrent.Akka.{ system => actorSystem }

  poller.start()

  private val scheduleNext = actorSystem.scheduler.scheduleOnce(delay) _

  private def printlnln(out: OutputStream)(s: String): Unit = out.write(s"$s\n\n".getBytes(StandardCharsets.UTF_8))

  def produce(out: OutputStream): Unit = {
    if (poller.isRunning) {
      listener.poll.foreach(printlnln(out))
      out.flush()
      scheduleNext(produce(out))
    } else {
      try {
        out.flush()
      } finally {
        out.close()
      }
    }
  }
}
