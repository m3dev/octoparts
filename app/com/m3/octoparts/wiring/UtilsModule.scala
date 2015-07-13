package com.m3.octoparts.wiring

import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.ZipkinServiceHolder
import com.m3.octoparts.logging.PartRequestLogger
import play.api.Play

/*
 Random common stuff that doesn't belong in other modules
*/
trait UtilsModule extends AppModule {

  lazy val playConfig = Play.configuration

  /**
   * Footgunney version of playConfig that throws if it can't find stuff
   */
  lazy val typesafeConfig = playConfig.underlying

  lazy val partsReqLogger: PartRequestLogger = PartRequestLogger

  implicit lazy val zipkinService: ZipkinServiceLike = ZipkinServiceHolder.ZipkinService

}