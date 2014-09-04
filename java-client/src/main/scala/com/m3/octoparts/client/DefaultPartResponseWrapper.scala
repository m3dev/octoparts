package com.m3.octoparts.client

import com.m3.octoparts.model.PartResponse
import org.slf4j.LoggerFactory

import scala.collection.convert.WrapAsJava

private[client] object DefaultPartResponseWrapper {
  private val Log = LoggerFactory.getLogger(classOf[DefaultPartResponseWrapper])
}

private[client] case class DefaultPartResponseWrapper(partResponse: PartResponse) extends PartResponseWrapper {

  import com.m3.octoparts.client.DefaultPartResponseWrapper._

  partResponse.warnings.foreach(Log.warn)

  def getErrors = WrapAsJava.seqAsJavaList(partResponse.errors)

  /**
   * @param defaultContents what to return if there were no contents
   */
  def getContents(defaultContents: String) = partResponse.contents.filter(_.length > 0 && partResponse.errors.isEmpty).getOrElse(defaultContents)

  def getCookies = WrapAsJava.seqAsJavaList(partResponse.cookies)

  def getPartId = partResponse.partId

  def getStatusCode = partResponse.statusCode.fold[Integer](null)(Int.box)

  def isRetrievedFromCache = partResponse.retrievedFromCache
}