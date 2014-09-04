package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model.config.{ PartParam, ShortPartParam }
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
  def combineParams(registeredParams: Set[PartParam], partRequestInfo: PartRequestInfo): Map[ShortPartParam, String] = {
    val requestParams = partRequestInfo.partRequest.params
    ensureUniqueParamKeys(requestParams)
    // processMeta comes first as its result can be overriden in the partRequest
    val combinedParams = processMeta(partRequestInfo.requestMeta) ++ requestParams.map(p => (p.key, p.value))
    val mappedParams = (for {
      p <- registeredParams
      v <- combinedParams.get(p.inputName) if v != null
    } yield p -> v).toMap

    validateParams(registeredParams, mappedParams)
    mappedParams.map {
      case (k, v) => k.shorter -> v
    }
  }

  @throws[IllegalArgumentException]
  private[service] def validateParams(registeredParams: Set[PartParam], mappedParams: Map[PartParam, String]): Unit = {
    val missingRequiredParams = registeredParams.filter {
      p => p.required && !mappedParams.contains(p)
    }
    if (missingRequiredParams.nonEmpty) {
      val missingParamsAsString = missingRequiredParams.map {
        _.inputName
      }.mkString(", ")
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
  def processMeta(meta: RequestMeta): Map[String, String] = {
    // nullchecks are needed because of the lack of upstream validation
    val metaProperties = Seq(
      "meta.requestUrl" -> meta.requestUrl,
      "meta.serviceId" -> meta.serviceId,
      "meta.sessionId" -> meta.sessionId,
      "meta.userAgent" -> meta.userAgent,
      "meta.userId" -> meta.userId)
    (for {
      t <- metaProperties if t._2 != null
      v <- t._2 if v != null
    } yield {
      t._1 -> v
    }).toMap
  }

  /**
   * Method for traversing the Set of params in the PartRequest to make sure that
   * there are no Params that have the same key. Throws an IllegalArgumentException
   * otherwise
   *
   * @param params params to check
   */
  def ensureUniqueParamKeys(params: Set[PartRequestParam]): Unit = {
    val repeatedKeys = repeatedParamKeys(params)
    if (repeatedKeys.nonEmpty)
      throw new IllegalArgumentException(s"The following parameters are given multiple times: ${repeatedKeys.mkString(",")}")
  }

  /**
   * Method for traversing the Set of params in the PartRequest and pulling out the
   * repeated keys
   *
   * @param params params to check
   */
  def repeatedParamKeys(params: Set[PartRequestParam]): Iterable[String] = {
    // Traverse once and count at the same time without temporary collections
    val paramsKeysWithCounts = params.foldLeft(Map.empty: Map[String, Int]) {
      (a, p) =>
        val key = p.key
        a.get(key) match {
          case Some(c) => a + (key -> (c + 1))
          case None => a + (key -> 1)
        }
    }
    paramsKeysWithCounts.collect({
      case (k, v) if v > 1 => k
    })
  }

}
