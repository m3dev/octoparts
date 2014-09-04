package com.m3.octoparts.client

import java.util.Collections

import com.m3.octoparts.model.Cookie

private[client] object EmptyPartResponseWrapper extends PartResponseWrapper {
  def getContents(defaultContents: String) = defaultContents

  def getCookies = Collections.emptyList[Cookie]

  def getErrors = Collections.emptyList[String]

  def getPartId = null

  def getStatusCode = null

  def isRetrievedFromCache = null
}