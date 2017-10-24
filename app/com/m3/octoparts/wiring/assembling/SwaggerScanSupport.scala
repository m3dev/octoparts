package com.m3.octoparts.wiring.assembling

import play.modules.swagger.SwaggerPluginImpl

trait SwaggerScanSupport {

  /**
   * Initiates Swagger
   */
  protected def initSwagger(components: ApplicationComponents): Unit = {
    new SwaggerPluginImpl(
      components.applicationLifecycle,
      components.router,
      components.application
    )
  }

}
