package com.m3.octoparts.wiring

import com.m3.octoparts.wiring.assembling.{ ApplicationComponents, BeforeStartupSupport }
import play.api.ApplicationLoader.Context
import play.api._

class OctopartsApplicationLoader
    extends ApplicationLoader
    with BeforeStartupSupport {

  def load(context: Context): Application = {
    val appComponents = components(context)
    appComponents.application
  }

  /**
   * The main bootstrapping method.
   *
   * Configures the logger, assembles the application components, and initiates pre-application
   * startup procedures.
   */
  def components(context: Context): ApplicationComponents = {
    Logger.configure(context.environment)
    val appComponents = new ApplicationComponents(context)
    beforeStart(appComponents)
    appComponents
  }

}
