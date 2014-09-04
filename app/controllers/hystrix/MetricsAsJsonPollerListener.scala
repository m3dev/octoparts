package controllers.hystrix

import java.util.concurrent.atomic.AtomicReference

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller

private class MetricsAsJsonPollerListener(queueSize: Int)
    extends HystrixMetricsPoller.MetricsAsJsonPollerListener {

  private val metrics = new AtomicReference[Seq[String]](Nil)

  private def flushMetrics: Seq[String] = metrics.getAndSet(Nil)

  @annotation.tailrec
  private def updateMetrics(f: Seq[String] => Seq[String]): Unit = {
    val oldValue = metrics.get()
    val newValue = f(oldValue)
    if (!metrics.compareAndSet(oldValue, newValue)) updateMetrics(f)
  }

  def handleJsonMetric(json: String): Unit = updateMetrics {
    oldMetrics =>
      {
        val newMetrics = oldMetrics :+ json
        if (newMetrics.size >= queueSize) throw new IllegalStateException("Queue full")
        newMetrics
      }
  }

  def poll: Seq[String] = flushMetrics match {
    case Nil => Seq("ping: ")
    case someLines => someLines.map(j => s"data: $j")
  }

}