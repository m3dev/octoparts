package com.m3.octoparts.client

import java.io.InputStream

import com.m3.octoparts.model.{ AggregateRequest, AggregateResponse }
import com.ning.http.client.{ AsyncCompletionHandler, Response }
import org.slf4j.LoggerFactory

private[client] object AggregateResponseExtractor {
  private val Log = LoggerFactory.getLogger(classOf[AggregateResponseExtractor])

  def deserialize(inputStream: InputStream): ResponseWrapper = {
    Option {
      OctopartsApiBuilder.Mapper.readValue[AggregateResponse](inputStream, classOf[AggregateResponse])
    } match {
      case None => EmptyResponseWrapper
      case Some(ag) => DefaultResponseWrapper(ag)
    }
  }
}

private[client] class AggregateResponseExtractor(aggregateRequest: AggregateRequest) extends AsyncCompletionHandler[ResponseWrapper] {

  import com.m3.octoparts.client.AggregateResponseExtractor._

  def onCompleted(response: Response): ResponseWrapper = {
    if (response.getStatusCode < 400) {
      deserialize(response.getResponseBodyAsStream)
    } else {
      Log.warn(s"Unexpected response status: '${response.getStatusText}' for request ${aggregateRequest.requestMeta.id}")
      EmptyResponseWrapper
    }
  }

  override def onThrowable(t: Throwable) {
    Log.warn(s"For request: ${aggregateRequest.requestMeta.id}", t)
  }
}