package com.m3.octoparts.wiring

import com.m3.octoparts.hystrix.HystrixHealthReporter

trait HystrixModule {

  lazy val hystrixReporter = HystrixHealthReporter

}
