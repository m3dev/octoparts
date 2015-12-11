package com.m3.octoparts.wiring

import com.beachape.zipkin.ZipkinHeaderFilter

trait FiltersModule extends UtilsModule {

  lazy val zipkinTracingFilter = ZipkinHeaderFilter(zipkinService)

}
