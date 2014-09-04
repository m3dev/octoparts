package com.m3.octoparts.ws

import com.m3.octoparts.model.RequestMeta

/**
 * Simple type class for building [[RequestMeta]]
 */
trait RequestMetaBuilder[-A] {
  def apply(obj: A): RequestMeta
}

object RequestMetaBuilder {

  /**
   * Returns a [[RequestMetaBuilder]] type class instance suitable for [[OctoClientLike]]#invoke[A]
   *
   * Example usage:
   *
   * {{{
   * case class User(id: Option[String])
   * case class EntityWithRequest[A](entity: A, request: RequestHeader)
   *
   * implicit val userWithReqBuilder = RequestMetaBuilder.from[EntityWithRequest[User]] { entWithReq =>
   *   val reqHeader = userWithReq.request
   *   val user = userWithReq.entity
   *   RequestMeta(
   *     id = reqHeader.id.toString,
   *     serviceId = Some("myService"),
   *     userId = user.userId,
   *     sessionId = reqHeader.cookies.get("sid").map(_.value),
   *     requestUrl = Option(reqHeader.uri),
   *     userAgent = reqHeader.headers.get("User-Agent")
   *   )
   * }
   *
   * val fAggResp = WSRequester.invoke(EntityWithRequest(myUser, req), partReqs)
   *
   * }}}
   */
  def from[A](transform: A => RequestMeta): RequestMetaBuilder[A] = new RequestMetaBuilder[A] {
    def apply(obj: A): RequestMeta = transform(obj)
  }

}