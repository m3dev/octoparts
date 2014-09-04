package com.m3.octoparts.client

import java.util.Collections

object EmptyResponseWrapper extends ResponseWrapper {
  def getPartResponse(partName: String) = EmptyPartResponseWrapper

  def getPartResponses = Collections.emptySet[PartResponseWrapper]

  def getResponseMeta = null

  def getMetaId = null
}