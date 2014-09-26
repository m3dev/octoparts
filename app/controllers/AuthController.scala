package controllers

import com.m3.octoparts.auth.{ PrincipalSessionPersistence, Principal }
import com.beachape.logging.LTSVLogger
import controllers.support.AuthSupport
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }

import scala.concurrent.Future
import scala.util.control.NonFatal

object AuthController extends Controller with AuthSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def logout = Action.async { implicit request =>
    val redirect = Redirect(routes.AdminController.listParts)
    authPlugin.fold {
      // No enabled auth plugin, so do the default behaviour, i.e. delete the Principal from Play session
      Future.successful(PrincipalSessionPersistence.deletePrincipalFromPlaySession(request, redirect))
    } { plugin =>
      // Let the plugin take care of it
      plugin.onLogout(request, redirect)
    }
  }

  def callback(origUrl: String) = Action.async { implicit request =>
    authPlugin.fold {
      // No enabled auth plugin. Just redirect to the the original URL
      Future.successful(Redirect(origUrl))
    } { plugin =>
      plugin.onAuthenticationCallback(request).flatMap { principal =>
        LTSVLogger.debug("Authentication completed for" -> principal.nickname, "Redirecting to" -> origUrl)
        plugin.savePrincipal(request, Redirect(origUrl), principal)
      }.recover {
        case NonFatal(e) =>
          LTSVLogger.warn(e, "Authentication failed for request" -> request, "request id" -> request.id, "request headers" -> request.headers)
          InternalServerError(s"Authentication failed: ${e.getMessage}")
      }
    }
  }

}
