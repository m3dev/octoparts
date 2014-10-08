package controllers.support

import com.m3.octoparts.auth.OctopartsAuthPlugin
import com.beachape.logging.LTSVLogger
import controllers.routes
import play.api.{ Logger, Play }
import play.api.mvc._

/**
 * A collection of ActionBuilders to support authentication and authorization.
 * Some of the auth logic is delegated to a plugin.
 */
trait AuthSupport
    extends AuthenticationCheckSupport
    with AuthorizationCheckSupport
    with DummyPrincipalSupport { self: Results =>

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  lazy val authPlugin: Option[OctopartsAuthPlugin] = Play.current.plugin[OctopartsAuthPlugin] match {
    case Some(plugin) if plugin.enabled =>
      LTSVLogger.info("Using auth plugin" -> plugin.getClass.getName)
      Some(plugin)
    case Some(disabledPlugin) =>
      LTSVLogger.info(s"Skipping auth for admin UI because auth plugin disabled" -> disabledPlugin.getClass.getName)
      None
    case None =>
      LTSVLogger.info("Msg" -> "No auth plugin found. Authentication/authorization for admin UI will be skipped.")
      None
  }

  /**
   * An action that requires the user to be authenticated.
   * If they are not authenticated:
   *  - if there is an enabled auth plugin, the request will be handled by that (e.g. redirected to an auth page).
   *  - otherwise they will be automatically logged in as a guest.
   */
  lazy val AuthenticatedAction = {
    authPlugin.fold {
      // No enabled auth plugin, so skip authentication and create a dummy principal
      Action andThen autoAuthenticateRequest
    } { plugin =>
      // Perform authentication check, delegating to plugin if not authenticated
      Action andThen authenticationCheckFilter(implicit req => plugin.onNotAuthenticated(req, routes.AuthController.callback(req.uri).absoluteURL()))
    }
  }

  /**
   * An action that requires a user to be both authenticated and authorized.
   *
   * Note that if there is no enabled auth plugin, authorization is skipped.
   */
  lazy val AuthorizedAction = {
    authPlugin.fold {
      // No enabled auth plugin, so skip authorization
      AuthenticatedAction
    } { plugin =>
      // Ask plugin to do authorization check, delegating to plugin if not authenticated
      AuthenticatedAction andThen authorizationCheckFilter(plugin.isAuthorized, plugin.onUnauthorized)
    }
  }

}
