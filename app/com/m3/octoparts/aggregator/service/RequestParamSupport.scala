package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model.config._
import com.m3.octoparts.model.{ PartRequestParam, RequestMeta }

import scala.collection.mutable

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
   */
  def combineParams(registeredParams: Set[PartParam], partRequestInfo: PartRequestInfo): Map[ShortPartParam, Seq[String]] = {
    val combinedParams = {
      val allParams = processMeta(partRequestInfo.requestMeta) ++ partRequestInfo.partRequest.params
      allParams.groupBy(_.key).mapValues(_.map(_.value))
    }
    val mappedParams = for {
      registeredParam <- registeredParams.toSeq
      values <- combinedParams.get(registeredParam.inputName)
    } yield {
      ParamMapping(registeredParam, values)
    }

    // will throw an IllegalArgumentException if a required parameter is missing.
    validateParams(registeredParams, mappedParams.map(_.partParam).toSet)

    (for {
      ParamMapping(partParam, values) <- mappedParams
    } yield {
      ShortPartParam(partParam) -> values
    }).toMap
  }

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

private case class ParamMapping(partParam: PartParam, values: Seq[String])