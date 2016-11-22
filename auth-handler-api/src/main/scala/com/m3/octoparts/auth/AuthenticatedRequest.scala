package com.m3.octoparts.auth

import play.api.mvc.{ Request, WrappedRequest }

/** An authenticated request, i.e. one with a valid principal */
case class AuthenticatedRequest[A](
  request: Request[A],
  principal: Principal
) extends WrappedRequest[A](request)
