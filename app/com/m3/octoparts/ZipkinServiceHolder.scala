package com.m3.octoparts

import java.net.InetAddress

import com.beachape.zipkin.services.{ BraveZipkinService, NoopZipkinService, ZipkinServiceLike }
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import play.api.mvc.RequestHeader
import play.api.{ Logger, Play }

import scala.util.{ Failure, Success, Try }

object ZipkinServiceHolder {

  private implicit val app = play.api.Play.current
  private implicit val ex = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def spanNamer(r: RequestHeader): String = {
    val tags = r.tags
    val pathPattern = tags.get(play.api.Routes.ROUTE_PATTERN)
    val path = pathPattern.getOrElse(r.path)
    s"${r.method} - $path"
  }

  val ZipkinService: ZipkinServiceLike = if (Play.isTest) {
    NoopZipkinService
  } else {
    val maybeService = for {
      zipkinHost <- Play.configuration.getString("zipkin.host")
      zipkinPort <- Play.configuration.getInt("zipkin.port")
      zipkinRate <- Play.configuration.getDouble("zipkin.sampleRate")
      env <- Play.configuration.getString("application.env")
    } yield {
      Try {
        val zipkinSpanCollector = new ZipkinSpanCollector(zipkinHost, zipkinPort)
        sys.addShutdownHook(zipkinSpanCollector.close())
        val currentHostName = InetAddress.getLocalHost.getHostName
        val currentRunningPort = Play.configuration.getInt("http.port").getOrElse(9000)
        new BraveZipkinService(
          hostIp = currentHostName,
          hostPort = currentRunningPort,
          serviceName = s"Octoparts - $env",
          collector = zipkinSpanCollector,
          clientTraceFilters = Seq(_.isSetParent_id),
          serverTraceFilters = Seq(
            { s =>
              val name = s.getName
              !(name.startsWith("OPTION") || name.startsWith("GET - /assets"))
            },
            { s => s.isSetParent_id || (zipkinRate > scala.util.Random.nextDouble()) }
          )
        )
      }

    }
    maybeService match {
      case Some(Success(zipkinService)) => zipkinService
      case Some(Failure(e)) => {
        Logger.error("Could not create the Zipkin service", e)
        NoopZipkinService
      }
      case None => {
        Logger.warn("Zipkin configs are missing in the current environment, falling back to NoopZipkinService")
        NoopZipkinService
      }
    }
  }

}
