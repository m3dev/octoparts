package controllers.support

import com.m3.octoparts.auth.AuthenticatedRequest
import com.beachape.logging.LTSVLogger
import play.api.Logger
import play.api.mvc.{ ActionFilter, Result }

import scala.concurrent.Future

trait AuthorizationCheckSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  /**
   * An action filter that checks whether the user is authorized.
   * If they are authorized, the action proceeds as normal.
   * Otherwise appropriate action is taken (e.g. return a 403 Forbidden response).
   *
   * @param isAuthorized function to decide whether a request is authorized
   * @param onUnauthorized what action to take when a user is not authenticated (e.g. a redirect)
   */
  protected[support] def authorizationCheckFilter(
    isAuthorized: AuthenticatedRequest[_] => Future[Boolean],
    onUnauthorized: AuthenticatedRequest[_] => Future[Result]) = new ActionFilter[AuthenticatedRequest] {

    def filter[A](inputReq: AuthenticatedRequest[A]) = {
      isAuthorized(inputReq).flatMap { authorized =>
        if (authorized) {
          LTSVLogger.debug("Accepting as authorized: principal" -> inputReq.principal.nickname)
          Future.successful(None)
        } else {
          onUnauthorized(inputReq).map(Some(_))
        }
      }
    }
  }

}
