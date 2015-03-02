package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.CacheOps
import com.m3.octoparts.cache.directive.CacheDirective
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.PartResponse
import com.twitter.zipkin.gen.Span

import scala.concurrent.Future

object DummyCacheOps extends CacheOps {
  override def increasePartVersion(partId: String)(implicit parentSpan: Span) = Future.successful(())

  override def putIfAbsent(directive: CacheDirective)(f: => Future[PartResponse])(implicit parentSpan: Span) = f

  override def increaseParamVersion(vpk: VersionedParamKey)(implicit parentSpan: Span) = Future.successful(())

  override def saveLater(partResponse: PartResponse, directive: CacheDirective)(implicit parentSpan: Span) = Future.successful(())
}
