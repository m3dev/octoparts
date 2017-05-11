package controllers.hystrix

import java.util.concurrent.LinkedBlockingQueue

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller

private class MetricsAsJsonPollerListener(
    queueSize: Int
) extends HystrixMetricsPoller.MetricsAsJsonPollerListener {

  private val metricsQueue = new LinkedBlockingQueue[String](queueSize)

  def handleJsonMetric(json: String): Unit = {
    if (metricsQueue.offer(json)) {
      ()
    } else {
      throw new IllegalStateException("Queue full")
    }
  }

  def poll: String = Option(metricsQueue.poll()) match {
    case None => "ping: "
    case Some(j) => s"data: $j"
  }

}
