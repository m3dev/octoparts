package com.m3.octoparts

import java.net.InetAddress

import com.beachape.zipkin.services.{ BraveZipkinService, NoopZipkinService, ZipkinServiceLike }
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import play.api.{ Logger, Play }

object ZipkinServiceHolder {

  private implicit val app = play.api.Play.current
  private implicit val ex = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val ZipkinService: ZipkinServiceLike = if (Play.isTest) {
    NoopZipkinService
  } else {
    val maybeService = for {
      zipkinHost <- Play.configuration.getString("zipkin.host")
      zipkinPort <- Play.configuration.getInt("zipkin.port")
      env <- Play.configuration.getString("application.env")
    } yield {
      val zipkinSpanCollector = new ZipkinSpanCollector(zipkinHost, zipkinPort)
      sys.addShutdownHook(zipkinSpanCollector.close())
      val currentHostName = InetAddress.getLocalHost.getHostName
      val currentRunningPort = Play.configuration.getInt("http.port").getOrElse(9000)
      new BraveZipkinService(
        currentHostName,
        currentRunningPort, s"Octoparts - $env",
        zipkinSpanCollector,
        Seq(
          !_.startsWith("OPTION"),
          !_.startsWith("POST - /eop"),
          !_.startsWith("GET - /static")
        )
      )
    }
    maybeService.getOrElse {
      Logger.warn("Zipkin configs are missing in the current environment, falling back to NoopZipkinService")
      NoopZipkinService
    }
  }

}
