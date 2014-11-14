package com.m3.octoparts.cache.versioning

import org.scalatest.{ Matchers, FunSpec }

import scala.util.Random

class LatestVersionCacheSpec extends FunSpec with Matchers {

  it("should make a growing, unique version ID") {
    val max = 10000
    val versionsSeq = for (i <- 1 to max) yield LatestVersionCache.makeNewVersion
    versionsSeq.distinct should equal(versionsSeq)
    // FIXME this test can fail when the VersionSequence overflows.
    versionsSeq.sorted should equal(versionsSeq)
  }

  it("should return None by default") {
    val latestVersionCache: LatestVersionCache = new InMemoryLatestVersionCache(100000)
    val versionedParamKeys = Seq(VersionedParamKey("id", "b", "some value"), VersionedParamKey("id", "a", "some other value"))
    val r = versionedParamKeys.map(latestVersionCache.getParamVersion)
    r.find {
      _.isDefined
    } should be('empty)
  }

  describe("InMemoryLatestVersionCache implementation") {

    val maxKeys = 100
    val latestVersionCache = new InMemoryLatestVersionCache(maxKeys)

    it("have internal caches that are limited by the #maxKeys argument") {
      for (i <- 0 to (2 * maxKeys)) latestVersionCache.updatePartVersion(i.toString, i + 1)
      for (i <- 0 to (2 * maxKeys)) latestVersionCache.updateParamVersion(VersionedParamKey(i.toString, Random.nextString(3), Random.nextString(3)), i + 1)
      latestVersionCache.partVersions.size() should be(maxKeys)
      latestVersionCache.paramVersions.size() should be(maxKeys)
    }

    it("should return proper last-updated values") {
      latestVersionCache.updatePartVersion("hello", 101)
      latestVersionCache.getPartVersion("hello").get should be(101L)
      latestVersionCache.updateParamVersion(VersionedParamKey("goodbye", "stop", "go"), 420)
      latestVersionCache.getParamVersion(VersionedParamKey("goodbye", "stop", "go")).get should be(420L)
    }

  }
}
