package com.m3.octoparts.wiring.assembling

import java.net.URL

import com.wordnik.swagger.config.{ ConfigFactory, FilterFactory, ScannerFactory }
import com.wordnik.swagger.core.SwaggerContext
import com.wordnik.swagger.core.filter.SwaggerSpecFilter
import com.wordnik.swagger.reader.ClassReaders
import pl.matisoft.swagger.{ PlayApiReader, PlayApiScanner, SwaggerPlugin }
import play.api.{ Application, Logger }
import play.api.inject.ApplicationLifecycle
import play.api.routing.Router
import play.modules.swagger.ApiListingCache

import scala.concurrent.{ ExecutionContext, Future }

trait SwaggerScanSupport {

  implicit def eCtx: ExecutionContext

  /**
   * Initiates Swagger
   */
  protected def initSwagger(components: ApplicationComponents): Unit = {
    val scanner = new SwaggerScanner(
      components.router,
      components.application,
      components.applicationLifecycle
    )
    scanner.scan()
  }

}

/**
 * Ghetto version of [[pl.matisoft.swagger.SwaggerPluginProvider]] without dependence on
 * injection
 */
class SwaggerScanner(
    router: Router,
    app: Application,
    lifecycle: ApplicationLifecycle
)(implicit eCtx: ExecutionContext) {

  val logger = Logger("swagger")

  def scan(): SwaggerPlugin = {
    lifecycle.addStopHook(() => Future {
      onStop()
    })

    onStart()
  }

  def onStart(): SwaggerPlugin = {
    val config = app.configuration
    logger.info("Swagger - starting initialisation...")

    val apiVersion = config.getOptional[String]("api.version") match {
      case None => "beta"
      case Some(value) => value
    }

    val basePath = config.getOptional[String]("swagger.api.basepath")
      .filter(path => !path.isEmpty)
      .map(getPathUrl)
      .getOrElse("http://localhost:9000")

    SwaggerContext.registerClassLoader(app.classloader)
    ConfigFactory.config.setApiVersion(apiVersion)
    ConfigFactory.config.setBasePath(basePath)
    ScannerFactory.setScanner(new PlayApiScanner(Option(router)))
    ClassReaders.reader = Some(new PlayApiReader(Option(router)))

    app.configuration.getOptional[String]("swagger.filter")
      .filter(p => !p.isEmpty)
      .foreach(loadFilter)

    val docRoot = ""
    ApiListingCache.listing(docRoot)

    logger.info("Swagger - initialization done.")
    new SwaggerPlugin()
  }

  def onStop() {
    ApiListingCache.cache = None
    logger.info("Swagger - stopped.")
  }

  def loadFilter(filterClass: String): Unit = {
    try {
      FilterFactory.filter = SwaggerContext.loadClass(filterClass).newInstance.asInstanceOf[SwaggerSpecFilter]
      logger.info(s"Setting swagger.filter to $filterClass")
    } catch {
      case ex: Exception => logger.error(s"Failed to load filter:$filterClass", ex)
    }
  }

  def getPathUrl(path: String): String = {
    try {
      val basePathUrl = new URL(path)
      logger.info(s"Basepath configured as:$path")
      path
    } catch {
      case ex: Exception =>
        logger.error(s"Misconfiguration - basepath not a valid URL:$path. Swagger abandoning initialisation!")
        throw ex
    }
  }

}
