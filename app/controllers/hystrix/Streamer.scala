package controllers.hystrix

import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream
import com.netflix.hystrix.serial.SerialHystrixDashboardData
import rx.RxReactiveStreams

// Wraps Hystrix's Dashboard stream into an Akka source
class Streamer(underlying: HystrixDashboardStream) {

  def source: Source[Array[Byte], NotUsed] = {
    val observable = underlying.observe()
    val publisher = RxReactiveStreams.toPublisher(observable)
    Source.fromPublisher(publisher).map(d => SerialHystrixDashboardData.toJsonString(d).getBytes(StandardCharsets.UTF_8))
  }
}