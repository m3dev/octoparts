package com.m3.octoparts.cache.directive

import org.scalatest.{ Matchers, FunSpec }
import com.m3.octoparts.cache.config._
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.support.mocks.ConfigDataMocks
import com.m3.octoparts.model.config._

class CacheDirectiveGeneratorSpec extends FunSpec with Matchers with ConfigDataMocks {
  val cacheDirectiveGenerator: CacheDirectiveGenerator = CacheDirectiveGenerator

  it("should make the most simple cache directive") {
    val cacheConfig = CacheConfig(None, Nil)
    val cacheDirective = cacheDirectiveGenerator.generateDirective("", Map.empty, cacheConfig)
    cacheDirective.versionedParamKeys should be('empty)
    cacheDirective.ttl should be(None)
  }

  it("should forward the part id, ttl and partRequestArgs in the cache directive") {
    val partId = "some part id"
    val ttl = FiniteDuration(5, TimeUnit.MINUTES)
    val cacheConfig = CacheConfig(Some(ttl), Nil)
    val partRequestArgs = Map(ShortPartParam("some key", ParamType.Query) -> Seq("some value"))
    val cacheDirective = cacheDirectiveGenerator.generateDirective(partId, partRequestArgs, cacheConfig)
    cacheDirective.versionedParamKeys should be('empty)
    cacheDirective.ttl should be(Some(ttl))
    cacheDirective.paramValues.size should be(partRequestArgs.size)
    cacheDirective.partId should be(partId)
  }

  it("should have version params") {
    val versionParamKeys = Seq("some key")
    val partRequestArgs = Map(
      ShortPartParam("some key", ParamType.Query) -> Seq("some value"),
      ShortPartParam("some other key", ParamType.Query) -> Seq("some other value")
    )
    val cacheConfig = CacheConfig(None, versionParamKeys)
    val cacheDirective = cacheDirectiveGenerator.generateDirective("some part id", partRequestArgs, cacheConfig)
    cacheDirective.versionedParamKeys should be(Seq(VersionedParamKey("some part id", "some key", "some value")))
  }
}
