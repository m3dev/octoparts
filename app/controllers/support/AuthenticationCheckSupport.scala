package controllers.support

import com.m3.octoparts.auth._
import play.api.mvc.{ ActionRefiner, Request, Result }

import scala.concurrent.Future

trait AuthenticationCheckSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def authHandler: Option[OctopartsAuthHandler]

  /**
   * An action refiner that acts like a filter, checking whether the user is authenticated.
   * If they have a valid principal in their session, it is extracted and attached to the request.
   * Otherwise appropriate action is taken to authenticate them (e.g. redirect them to an OpenID auth page).
   *
   * @param onUnauthenticated what action to take when a user is not authenticated (e.g. a redirect)
   */
  protected[support] def authenticationCheckFilter(onUnauthenticated: Request[_] => Future[Result]) = new ActionRefiner[Request, AuthenticatedRequest] {

    def refine[A](inputReq: Request[A]) = {
      extractPrincipal(inputReq).flatMap { maybePrincipal =>
        maybePrincipal.fold[Future[Either[Result, AuthenticatedRequest[A]]]] {
          // No valid principal in session, so authentication fails
          onUnauthenticated(inputReq).map(Left(_))
        } { principal =>
          Future.successful(Right(AuthenticatedRequest(inputReq, principal)))
        }
      }
    }

  }

  private def extractPrincipal(request: Request[_]): Future[Option[Principal]] = {
    authHandler.fold {
      // No auth plugin, so do it the default way, using Play session cookie
      Future.successful(PrincipalSessionPersistence.extractPrincipalFromPlaySession(request.session))
    } { plugin =>
      // Let the plugin take care of it
      plugin.loadPrincipal(request)
    }
  }

}
