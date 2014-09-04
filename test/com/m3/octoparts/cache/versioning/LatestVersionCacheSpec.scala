package com.m3.octoparts.cache.versioning

import org.scalatest.{ Matchers, FunSpec }

class LatestVersionCacheSpec extends FunSpec with Matchers {

  it("should make a growing, unique version ID") {
    val max = 10000
    val versionsSeq = for (i <- 1 to max) yield LatestVersionCache.makeNewVersion
    versionsSeq.distinct should equal(versionsSeq)
    // FIXME this test can fail when the VersionSequence overflows.
    versionsSeq.sorted should equal(versionsSeq)
  }

  it("should return None by default") {
    val latestVersionCache: LatestVersionCache = new InMemoryLatestVersionCache
    val versionedParamKeys = Seq(VersionedParamKey("id", "b", "some value"), VersionedParamKey("id", "a", "some other value"))
    val r = versionedParamKeys.map(latestVersionCache.getParamVersion)
    r.find {
      _.isDefined
    } should be('empty)
  }
}
