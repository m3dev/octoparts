package com.m3.octoparts.ws

import java.nio.charset.StandardCharsets

import com.m3.octoparts.model.{ PartRequestParam, PartRequest }
import play.api.http.Writeable

/**
 * An enrichment for [[PartRequest]] to offer nice helpers
 */
object PartRequestEnrichment {

  private val BodyParamKey: String = "body"

  implicit class RichPartRequest(val partReq: PartRequest) extends AnyVal {

    /**
     * Given a body type with a [[Writeable]] available, returns a PartRequest with
     * the properly formatted body param [[PartRequestParam]] (key of body, and only one)
     */
    def withBody[A: Writeable](body: A): PartRequest = {
      val bodyAsString = new String(implicitly[Writeable[A]].transform(body), StandardCharsets.UTF_8)
      val paramsWithBody = partReq.params.filterNot(_.key == BodyParamKey) :+ PartRequestParam(BodyParamKey, bodyAsString)
      partReq.copy(params = paramsWithBody)
    }

  }

}
