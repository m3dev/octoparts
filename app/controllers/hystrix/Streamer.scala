package controllers.hystrix

import java.io.OutputStream
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller

import scala.concurrent.duration.FiniteDuration

private class Streamer(
    poller: HystrixMetricsPoller,
    listener: MetricsAsJsonPollerListener,
    delay: FiniteDuration
)(implicit actorSystem: ActorSystem) {

  import actorSystem.dispatcher

  poller.start()

  private val scheduleNext = actorSystem.scheduler.scheduleOnce(delay) _

  private def printlnln(
    out: OutputStream
  )(s: String): Unit = out.write(s"$s\n\n".getBytes(StandardCharsets.UTF_8))

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
