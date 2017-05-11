package controllers.hystrix

import java.nio.charset.StandardCharsets

import akka.stream.stage._
import akka.stream.{ Attributes, Outlet, SourceShape }
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller
import play.api.Logger

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

private class Streamer(
    poller: HystrixMetricsPoller,
    listener: MetricsAsJsonPollerListener,
    delay: FiniteDuration,
    cleanup: () => Unit
) extends GraphStage[SourceShape[Array[Byte]]] {

  private val out = Outlet[Array[Byte]]("HystrixDataStreamer.Out")
  val shape: SourceShape[Array[Byte]] = SourceShape(out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private val logger = Logger(this.getClass)

      setHandler(
        out,
        new OutHandler {
          def onPull(): Unit =
            if (poller.isRunning) {
              val s = listener.poll
              push(out, s"$s\n\n".getBytes(StandardCharsets.UTF_8))
            } else {
              completeStage()
            }
        }
      )

      override def postStop(): Unit = {
        try {
          cleanup()
        } catch {
          case NonFatal(t) => logger.error(s"Failed to cleanup", t)
        }
      }
    }

}
