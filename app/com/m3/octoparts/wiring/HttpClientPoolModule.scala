package com.m3.octoparts.wiring

import com.m3.octoparts.http.HttpClientPool

trait HttpClientPoolModule extends UtilsModule {

  lazy val httpClientPool: HttpClientPool = {
    val pool = new HttpClientPool
    sys.addShutdownHook {
      pool.shutdown()
    }
    pool
  }

}