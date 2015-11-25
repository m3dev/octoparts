package com.m3.octoparts.wiring

import com.m3.octoparts.http.HttpClientPool
import com.softwaremill.macwire._

trait HttpClientPoolModule extends UtilsModule {

  lazy val httpClientPool: HttpClientPool = {
    val pool = wire[HttpClientPool]
    sys.addShutdownHook {
      pool.shutdown()
    }
    pool
  }

}