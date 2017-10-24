package com.m3.octoparts.wiring

import java.net.InetAddress

import com.beachape.zipkin.services.{ BraveZipkinService, NoopZipkinService, ZipkinServiceLike }
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector
import com.m3.octoparts.logging.PartRequestLogger
import com.m3.octoparts.util.OctoMetricsImpl
import play.api.Mode
import play.api._
import play.api.inject.ApplicationLifecycle

import scala.util.{ Failure, Success, Try }
import com.softwaremill.macwire._

import scala.concurrent.ExecutionContext

/*
 Random common stuff that doesn't belong in other modules
*/
trait UtilsModule {

  def mode: Mode

  def configuration: Configuration

  def applicationLifecycle: ApplicationLifecycle

  def executionContext: ExecutionContext

  implicit lazy val metrics = wire[OctoMetricsImpl]

  /*
   * Footgunney version of playConfig that throws if it can't find stuff
   */
  lazy val typesafeConfig = configuration.underlying

  lazy val partsReqLogger: PartRequestLogger = PartRequestLogger

  implicit lazy val zipkinService: ZipkinServiceLike = {
    implicit def eCtx = executionContext
    if (mode == Mode.Test) {
      NoopZipkinService
    } else {
      val maybeService = for {
        zipkinHost <- configuration.getOptional[String]("zipkin.host")
        zipkinPort <- configuration.getOptional[Int]("zipkin.port")
        zipkinRate <- configuration.getOptional[Double]("zipkin.sampleRate")
        env <- configuration.getOptional[String]("application.env")
      } yield {
        Try {
          val zipkinSpanCollector = new ZipkinSpanCollector(zipkinHost, zipkinPort)
          sys.addShutdownHook(zipkinSpanCollector.close())
          val currentHostName = InetAddress.getLocalHost.getHostName
          val currentRunningPort = configuration.getOptional[Int]("http.port").getOrElse(9000)
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
        case Some(Success(instantiatedZipkinService)) => instantiatedZipkinService
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