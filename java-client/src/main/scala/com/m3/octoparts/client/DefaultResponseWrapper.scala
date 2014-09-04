package com.m3.octoparts.client

import com.m3.octoparts.model.{ AggregateResponse, PartResponse, ResponseMeta }

import scala.collection.convert.WrapAsJava

private[client] case class DefaultResponseWrapper(aggregateResponse: AggregateResponse) extends ResponseWrapper {

  private def wrap(partResponse: Option[PartResponse]): PartResponseWrapper =
    partResponse.fold[PartResponseWrapper](EmptyPartResponseWrapper)(DefaultPartResponseWrapper.apply)

  def getPartResponse(partName: String): PartResponseWrapper = wrap(aggregateResponse.findPart(partName))

  def getPartResponses: java.lang.Iterable[PartResponseWrapper] = {
    WrapAsJava.asJavaCollection(aggregateResponse.responses.map(Option.apply).map(wrap))
  }

  def getResponseMeta: ResponseMeta = aggregateResponse.responseMeta

  def getMetaId: String = Option(getResponseMeta).fold[String](null)(_.id)
}