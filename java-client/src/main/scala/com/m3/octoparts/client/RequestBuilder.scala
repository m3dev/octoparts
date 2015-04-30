package com.m3.octoparts.client

import java.util
import javax.annotation.{ Nonnull, Nullable }

import com.m3.octoparts.model._

import scala.collection.convert.WrapAsScala

private[client] class RequestBuilder(requestMeta: RequestMeta) {
  private[this] val partRequests = Seq.newBuilder[PartRequest]

  def build = partRequests.synchronized(AggregateRequest(requestMeta, partRequests.result()))

  @Nonnull def newPart(@Nonnull partId: String, @Nullable id: String) = new PartBuilder(partId, Option(id))

  class PartBuilder private[client] (partId: String, id: Option[String]) {
    private[this] val params = Seq.newBuilder[PartRequestParam]

    @Nonnull def addParam(@Nonnull key: String, @Nonnull value: String): PartBuilder = {
      params.synchronized(params += PartRequestParam(key, value))
      this
    }

    @Nonnull def addParams(@Nonnull someParams: util.Map[String, String]): PartBuilder = {
      val manyParams = for ((k, v) <- WrapAsScala.mapAsScalaMap(someParams) if k != null && v != null) yield PartRequestParam(k, v)
      params.synchronized(params ++= manyParams)
      this
    }

    def build() = {
      val builtParams = params.synchronized(params.result())
      partRequests.synchronized(partRequests += PartRequest(partId, id, builtParams))
    }
  }

}
