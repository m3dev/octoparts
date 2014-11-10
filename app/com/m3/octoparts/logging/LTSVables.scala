package com.m3.octoparts.logging

import com.beachape.logging.LTSVable
import com.m3.octoparts.model._

object LTSVables extends LogUtil {

  implicit val requestMetaLTSVable: LTSVable[RequestMeta] = new LTSVable[RequestMeta] {
    def toPairs(o: RequestMeta): Seq[(String, Any)] = Seq(
      "RequestMeta id" -> o.id,
      "RequestMeta requestUrl" -> o.requestUrl,
      "RequestMeta timeout" -> o.timeout.map(toRelevantUnit(_)),
      "RequestMeta serviceId" -> o.serviceId,
      "RequestMeta sessionId" -> o.sessionId,
      "RequestMeta" -> o
    )
  }

  implicit val aggregateRequestLTSVable: LTSVable[AggregateRequest] = new LTSVable[AggregateRequest] {
    def toPairs(o: AggregateRequest): Seq[(String, Any)] =
      requestMetaLTSVable.toPairs(o.requestMeta) :+ "Requests length" -> o.requests.length
  }

  implicit val responseMetaLTSVable: LTSVable[ResponseMeta] = new LTSVable[ResponseMeta] {
    def toPairs(o: ResponseMeta): Seq[(String, Any)] = Seq(
      "ResponseMeta id" -> o.id,
      "ResponseMeta processTime" -> toRelevantUnit(o.processTime),
      "ResponseMeta" -> o
    )
  }

  implicit val aggregateResponseLTSVable: LTSVable[AggregateResponse] = new LTSVable[AggregateResponse] {
    def toPairs(o: AggregateResponse): Seq[(String, Any)] =
      responseMetaLTSVable.toPairs(o.responseMeta) :+ "Responses length" -> o.responses.length
  }

  implicit val partResponseLTSVable: LTSVable[PartResponse] = new LTSVable[PartResponse] {
    def toPairs(o: PartResponse): Seq[(String, Any)] = Seq(
      "PartResponse id" -> o.id,
      "PartResponse partId" -> o.partId,
      "PartResponse errors" -> o.errors,
      "PartResponse warnings" -> o.warnings,
      "PartResponse statusCode" -> o.statusCode,
      "PartResponse cacheControl" -> o.cacheControl,
      "PartResponse retrievedFromCache" -> o.retrievedFromCache
    )
  }

  /**
   * If we need more, add more of these
   */
  implicit def pairsLTSVable[A: LTSVable, B: LTSVable]: LTSVable[(A, B)] = new LTSVable[(A, B)] {
    def toPairs(o: (A, B)): Seq[(String, Any)] = implicitly[LTSVable[A]].toPairs(o._1) ++ implicitly[LTSVable[B]].toPairs(o._2)
  }

}
