package controllers

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.auth.{ OctopartsAuthHandler, PrincipalSessionPersistence }
import controllers.support.AuthSupport
import play.api.mvc.{ AbstractController, ControllerComponents }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class AuthController(
  val authHandler: Option[OctopartsAuthHandler],
  controllerComponents: ControllerComponents
)(implicit val eCtx: ExecutionContext) extends AbstractController(controllerComponents)
    with AuthSupport {

  def logout = Action.async { implicit request =>
    val redirect = Redirect(routes.AdminController.listParts)
    authHandler.fold {
      // No enabled auth plugin, so do the default behaviour, i.e. delete the Principal from Play session
      Future.successful(
        PrincipalSessionPersistence.deletePrincipalFromPlaySession(request, redirect)
      )
    } { plugin =>
      // Let the plugin take care of it
      plugin.onLogout(request, redirect)
    }
  }

  def callback(origUrl: String) = Action.async { implicit request =>
    authHandler.fold {
      // No enabled auth plugin. Just redirect to the the original URL
      Future.successful(Redirect(origUrl))
    } { plugin =>
      plugin
        .onAuthenticationCallback(request)
        .flatMap { principal =>
          LTSVLogger.debug(
            "Authentication completed for" -> principal.nickname,
            "Redirecting to" -> origUrl
          )
          plugin.savePrincipal(request, Redirect(origUrl), principal)
        }
        .recover {
          case NonFatal(e) =>
            LTSVLogger.warn(
              e,
              "Authentication failed for request" -> request,
              "request id" -> request.id,
              "request headers" -> request.headers
            )
            InternalServerError(s"Authentication failed: ${e.getMessage}")
        }
    }
  }

}
