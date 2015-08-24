package com.m3.octoparts.client

import com.ning.http.client.{ AsyncCompletionHandler, Response }
import org.slf4j.LoggerFactory

private[client] object OKResponseExtractor {
  private final val Log = LoggerFactory.getLogger(classOf[OKResponseExtractor])
}

private[client] class OKResponseExtractor(uri: String) extends AsyncCompletionHandler[java.lang.Boolean] {

  import com.m3.octoparts.client.OKResponseExtractor._

  def onCompleted(response: Response): java.lang.Boolean = {
    response.getStatusCode match {
      case ok if ok < 400 =>
        Log.trace(s"$uri -> ${response.getResponseBody}")
        true
      case 404 =>
        Log.warn(s"404: ${response.getResponseBody}' for path $uri")
        false
      case _ =>
        Log.warn(s"Unexpected response status: '${response.getStatusText}' for path $uri")
        false
    }
  }

  override def onThrowable(t: Throwable) = {
    Log.warn(s"For path: $uri", t)
  }
}