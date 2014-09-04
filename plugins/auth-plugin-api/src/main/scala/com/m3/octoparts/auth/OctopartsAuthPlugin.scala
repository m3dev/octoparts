package com.m3.octoparts.auth

import play.api.Plugin
import play.api.mvc.{ Request, Result }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Authentication plugin for Octoparts should implement this trait.
 *
 * e.g.
 *
 * {{{
 *   class MyAuthPlugin(application: Application) extends OctopartsAuthPlugin {
 *     def onNotAuthenticated(request: Request[_])(implicit exec: ExecutionContext) = {
 *       // redirect to authentication server ...
 *     }
 *
 *     def isAuthorized(authenticatedRequest: AuthenticatedRequest[_])(implicit exec: ExecutionContext) = {
 *       // Only authorize admin users
 *       Future.successful(authenticatedRequest.principal.roles.exists("admin")
 *     }
 *
 *     def onUnauthorized(authenticatedRequest: AuthenticatedRequest[_])(implicit exec: ExecutionContext) = {
 *       Future.successful(Forbidden)
 *     }
 *
 *     def onAuthenticationCallback(request:Request)(implicit exec: ExecutionContext) = {
 *       val nickname = ...
 *       Future.successful(Principal(nickname, email, roles))
 *     }
 *   }
 * }}}
 */
trait OctopartsAuthPlugin extends Plugin {

  /**
   * What action to take when a request is not authenticated, e.g. redirect to an authentication server.
   *
   * @param request The unauthenticated request
   * @param callbackUrl An absolute URL that you can use for a post-authentication callback.
   *                    It will include the current (relative) URL as a query parameter,
   *                    e.g. "http://octoparts:9000/auth/callback?origUrl=%2Fadmin"
   */
  def onNotAuthenticated(request: Request[_], callbackUrl: String)(implicit exec: ExecutionContext): Future[Result]

  /**
   * Whether the given principal is authorized to use the Octoparts admin UI.
   *
   * @param authenticatedRequest an authenticated request with a [[com.m3.octoparts.auth.Principal]]
   * @return your decision
   */
  def isAuthorized(authenticatedRequest: AuthenticatedRequest[_])(implicit exec: ExecutionContext): Future[Boolean]

  /**
   * What action to take when a request is authenticated but the principal is not authorized.
   *
   * @param authenticatedRequest an authenticated request with a [[com.m3.octoparts.auth.Principal]]
   */
  def onUnauthorized(authenticatedRequest: AuthenticatedRequest[_])(implicit exec: ExecutionContext): Future[Result]

  /**
   * Use a post-authentication callback from an external authentication provider to construct a [[com.m3.octoparts.auth.Principal]]
   *
   * @return a Principal constructed from the HTTP request
   */
  def onAuthenticationCallback(request: Request[_])(implicit exec: ExecutionContext): Future[Principal]

  /**
   * When the user logs out, they are redirected to the top page.
   * This callback is your chance to clean up their session, e.g. delete cookies.
   * The default implementation removes the Principal from the Play session cookie.
   */
  def onLogout(request: Request[_], result: Result): Future[Result] = {
    Future.successful(PrincipalSessionPersistence.deletePrincipalFromPlaySession(request, result))
  }

  /**
   * Try to extract a Principal from the session.
   * The default implementation extracts the JSON-serialized Principal from the Play session cookie.
   */
  def loadPrincipal(request: Request[_])(implicit exec: ExecutionContext): Future[Option[Principal]] = {
    Future.successful(PrincipalSessionPersistence.extractPrincipalFromPlaySession(request.session))
  }

  /**
   * This is called when redirecting to the original URL after authentication has successfully completed.
   * This callback is your change to save the principal to a session.
   * The default implementation saves it as JSON to the Play session cookie.
   */
  def savePrincipal(request: Request[_], result: Result, principal: Principal): Future[Result] = {
    Future.successful(PrincipalSessionPersistence.savePrincipalToPlaySession(request, result, principal))
  }

}

