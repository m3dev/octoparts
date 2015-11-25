package com.m3.octoparts.wiring

import java.net.InetAddress

import com.beachape.zipkin.services.{ BraveZipkinService, NoopZipkinService, ZipkinServiceLike }
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import com.kenshoo.play.metrics.MetricsImpl

import com.m3.octoparts.logging.PartRequestLogger
import play.api.Mode.Mode
import play.api._
import play.api.inject.ApplicationLifecycle

import scala.util.{ Failure, Success, Try }

import com.softwaremill.macwire._

/*
 Random common stuff that doesn't belong in other modules
*/
trait UtilsModule {

  def mode: Mode

  def configuration: Configuration

  def applicationLifecycle: ApplicationLifecycle

  implicit lazy val metrics = wire[MetricsImpl]

  /*
   * Footgunney version of playConfig that throws if it can't find stuff
   */
  lazy val typesafeConfig = configuration.underlying

  lazy val partsReqLogger: PartRequestLogger = PartRequestLogger

  implicit lazy val zipkinService: ZipkinServiceLike = {
    import scala.concurrent.ExecutionContext.Implicits.global
    if (mode == Mode.Test) {
      NoopZipkinService
    } else {
      val maybeService = for {
        zipkinHost <- configuration.getString("zipkin.host")
        zipkinPort <- configuration.getInt("zipkin.port")
        zipkinRate <- configuration.getDouble("zipkin.sampleRate")
        env <- configuration.getString("application.env")
      } yield {
        Try {
          val zipkinSpanCollector = new ZipkinSpanCollector(zipkinHost, zipkinPort)
          sys.addShutdownHook(zipkinSpanCollector.close())
          val currentHostName = InetAddress.getLocalHost.getHostName
          val currentRunningPort = configuration.getInt("http.port").getOrElse(9000)
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

}