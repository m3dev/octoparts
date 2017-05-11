package controllers.support

import com.m3.octoparts.auth.{ AuthenticatedRequest, OctopartsAuthHandler, Principal, PrincipalSessionPersistence }
import play.api.libs.json.Json
import play.api.mvc.{ ActionFunction, Request, Result }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

trait DummyPrincipalSupport {

  implicit def eCtx: ExecutionContext

  /**
   * Create a new guest principal.
   */
  private def createDummyPrincipal(request: Request[_]): Principal =
    Principal(
      s"guest${Random.nextInt()}",
      "guest",
      s"guest@${request.remoteAddress}",
      roles = Nil
    )

  /**
   * An ActionTransformer that creates a guest principal automatically if it doesn't find one in the session.
   * i.e. the user does not need to authenticate - anybody can access the site.
   */
  protected[support] val autoAuthenticateRequest = new ActionFunction[Request, AuthenticatedRequest] {

    protected def executionContext: ExecutionContext = eCtx

    def invokeBlock[A](
      inputReq: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]
    ) = {
      PrincipalSessionPersistence.extractPrincipalFromPlaySession(inputReq.session)
        .fold[Future[Result]] {
          // No valid principal in session, so create a new guest principal and attach it to the request
          val principal = createDummyPrincipal(inputReq)
          implicit val authedRequest = AuthenticatedRequest(inputReq, principal)

          // Run the block, then add the newly created principal to the session cookie
          block(authedRequest).map(result =>
            PrincipalSessionPersistence.savePrincipalToPlaySession(inputReq, result, principal))
        } { principal =>
          // Principal found in session - proceed normally
          val authedRequest = AuthenticatedRequest(inputReq, principal)
          block(authedRequest)
        }
    }
  }

}
