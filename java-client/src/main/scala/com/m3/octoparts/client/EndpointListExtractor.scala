package com.m3.octoparts.client

import java.io.InputStream
import java.{ util => ju }

import com.m3.octoparts.model.config.json.HttpPartConfig
import com.ning.http.client.{ AsyncCompletionHandler, Response }
import org.slf4j.LoggerFactory

private[client] object EndpointListExtractor extends EndpointListExtractor {
  private val Log = LoggerFactory.getLogger(classOf[EndpointListExtractor])

  private val ListOfConfigsType = OctopartsApiBuilder.Mapper.getTypeFactory.constructCollectionType(classOf[ju.List[HttpPartConfig]], classOf[HttpPartConfig])

  def deserialize(inputStream: InputStream): ju.List[HttpPartConfig] = {
    Option {
      OctopartsApiBuilder.Mapper.readValue[ju.List[HttpPartConfig]](inputStream, ListOfConfigsType)
    } match {
      case None => ju.Collections.emptyList()
      case Some(configs) => configs
    }
  }
}

private class EndpointListExtractor extends AsyncCompletionHandler[ju.List[HttpPartConfig]] {

  import com.m3.octoparts.client.EndpointListExtractor._

  override def onCompleted(response: Response): ju.List[HttpPartConfig] = {
    if (response.getStatusCode < 400) {
      deserialize(response.getResponseBodyAsStream)
    } else {
      Log.warn(s"Unexpected response status: '${response.getStatusText}' for endpoint list")
      ju.Collections.emptyList()
    }
  }

}
