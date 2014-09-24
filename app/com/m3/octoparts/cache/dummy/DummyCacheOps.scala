package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.CacheOps
import com.m3.octoparts.cache.directive.CacheDirective
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.PartResponse

import scala.concurrent.Future

object DummyCacheOps extends CacheOps {
  override def increasePartVersion(partId: String) = Future.successful(())

  override def putIfAbsent(directive: CacheDirective)(f: => Future[PartResponse]) = f

  override def increaseParamVersion(vpk: VersionedParamKey) = Future.successful(())

  override def saveLater(partResponse: PartResponse, directive: CacheDirective) = Future.successful(())
}
