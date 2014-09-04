package com.m3.octoparts.client

import com.m3.octoparts.model.{ AggregateRequest, AggregateResponse }
import com.ning.http.client.{ AsyncCompletionHandler, Response }
import org.slf4j.LoggerFactory

private[client] object AggregateResponseExtractor {
  private val Log = LoggerFactory.getLogger(classOf[AggregateResponseExtractor])
}

private[client] class AggregateResponseExtractor(aggregateRequest: AggregateRequest) extends AsyncCompletionHandler[ResponseWrapper] {

  import com.m3.octoparts.client.AggregateResponseExtractor._

  def onCompleted(response: Response): ResponseWrapper = {
    if (response.getStatusCode < 400) {
      val ag = OctopartsApiBuilder.Mapper.readValue(response.getResponseBodyAsStream, classOf[AggregateResponse])
      Option(ag).fold[ResponseWrapper](EmptyResponseWrapper)(DefaultResponseWrapper.apply)
    } else {
      Log.warn(s"Unexpected response status: '${response.getStatusText}' for request ${aggregateRequest.requestMeta.id}")
      EmptyResponseWrapper
    }
  }

  override def onThrowable(t: Throwable) {
    Log.warn(s"For request: ${aggregateRequest.requestMeta.id}", t)
  }
}