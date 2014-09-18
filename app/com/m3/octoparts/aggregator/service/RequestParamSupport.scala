package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model.config._
import com.m3.octoparts.model.{ PartRequestParam, RequestMeta }

/**
 * Trait to support generic operations on PartRequestInfo components like
 * RequestMeta and PartRequest
 */
trait RequestParamSupport {

  /**
   * Combines and validates registered parameters across Request Meta
   * and the PartRequest parameters
   *
   * Parameters are combined across PartRequestInfo.partRequest and
   * RequestMeta (where e.g. sessionId a and userId are held). They are
   * then compared to the registeredParams for this Handler. If any
   * params that are required are missing, then an exception is thrown.
   *
   * @param partRequestInfo Part request info
   * @return Map[Param, String]
   */
  @throws[IllegalArgumentException]
  def combineParams(registeredParams: Set[PartParam], partRequestInfo: PartRequestInfo): Seq[ShortPartParamValue] = {
    val combinedParams = processMeta(partRequestInfo.requestMeta) ++ partRequestInfo.partRequest.params
    val mappedParams = for {
      partRequestParam <- combinedParams
      registeredParam <- registeredParams.find(_.inputName == partRequestParam.key)
    } yield {
      ParamMapping(registeredParam, partRequestParam.value)
    }

    validateParams(registeredParams, mappedParams.map(_.partParam).toSet)

    for {
      (partParam, mappedParamsFor) <- mappedParams.groupBy(_.partParam).toSeq
    } yield {
      ShortPartParamValue(ShortPartParam(partParam), mappedParamsFor.map(_.value))
    }
  }

  @throws[IllegalArgumentException]
  private[service] def validateParams(registeredParams: Set[PartParam], mappedParams: Set[PartParam]): Unit = {
    val missingRequiredParams = (registeredParams -- mappedParams).filter(_.required)
    if (missingRequiredParams.nonEmpty) {
      val missingParamsAsString = missingRequiredParams.map(_.inputName).mkString(", ")
      throw new IllegalArgumentException(s"The following parameters are missing: $missingParamsAsString")
    }
  }

  /**
   * Extracts out commonly used values from RequestMeta
   * like userId and sessionId so that we can address them in
   * a map
   *
   * In the future maybe we can recursively create a map that looks up
   * declared fields and values on the meta case class
   *
   * @param meta RequestMeta
   * @return Map[String, String]
   */
  def processMeta(meta: RequestMeta): Seq[PartRequestParam] = for {
    (meta, mbValue) <- Seq(
      "meta.requestUrl" -> meta.requestUrl,
      "meta.serviceId" -> meta.serviceId,
      "meta.sessionId" -> meta.sessionId,
      "meta.userAgent" -> meta.userAgent,
      "meta.userId" -> meta.userId
    )
    value <- mbValue
  } yield {
    PartRequestParam(meta, value)
  }
}

private case class ParamMapping(partParam: PartParam, value: String)