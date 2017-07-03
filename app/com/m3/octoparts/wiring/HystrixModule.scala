package com.m3.octoparts.wiring

import com.m3.octoparts.hystrix.HystrixHealthReporter
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream
import controllers.hystrix.Streamer

trait HystrixModule {

  lazy val hystrixReporter = HystrixHealthReporter
  lazy val hystrixStreamer = new Streamer(HystrixDashboardStream.getInstance())

}
