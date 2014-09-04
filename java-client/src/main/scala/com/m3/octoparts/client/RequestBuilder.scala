package com.m3.octoparts.client

import java.util
import javax.annotation.{ Nonnull, Nullable }

import com.m3.octoparts.model.{ AggregateRequest, PartRequest, PartRequestParam, RequestMeta }

import scala.collection.convert.WrapAsScala

private[client] case class RequestBuilder(requestMeta: RequestMeta) {
  rb =>
  private var partRequests = Seq.empty[PartRequest]

  def build = AggregateRequest(requestMeta, partRequests)

  def countParts: Int = partRequests.size

  @Nonnull def newPart(@Nonnull partId: String, @Nullable id: String) = PartBuilder(partId, Option(id))

  case class PartBuilder private[client] (partId: String, id: Option[String]) {
    private var params = Set.empty[PartRequestParam]

    @Nonnull def addParam(@Nonnull key: String, @Nullable value: String): PartBuilder = {
      this.synchronized {
        params = params + PartRequestParam(key, value)
      }
      this
    }

    @Nonnull def addParams(@Nonnull someParams: util.Map[String, String]): PartBuilder = {
      val manyParams = for (e <- WrapAsScala.mapAsScalaMap(someParams) if e._1 != null) yield PartRequestParam(e._1, e._2)
      this.synchronized {
        params = params ++ manyParams
      }
      this
    }

    def build() {
      rb.synchronized {
        partRequests = partRequests :+ PartRequest(partId, id, params)
      }
    }
  }

}
