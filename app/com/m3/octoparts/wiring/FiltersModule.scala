package com.m3.octoparts.wiring

import com.beachape.zipkin.ZipkinHeaderFilter
import play.api.BuiltInComponents

trait FiltersModule extends UtilsModule { this: BuiltInComponents =>

  lazy val zipkinTracingFilter = ZipkinHeaderFilter(zipkinService)

}
