package com.m3.octoparts.cache.client

import java.util.concurrent.Executors

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.versioning.{ LatestVersionCache, InMemoryLatestVersionCache }
import org.scalatest._
import com.m3.octoparts.cache.key.{ PartCacheKey, MemcachedKeyGenerator }
import com.m3.octoparts.cache.directive.CacheDirective
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import scala.language.postfixOps
import shade.memcached.{ Memcached, Codec }
import java.io.IOException
import org.joda.time.DateTimeUtils
import com.m3.octoparts.model.{ CacheControl, PartResponse }

class CacheClientSpec extends FunSpec with Matchers with ScalaFutures with Eventually with BeforeAndAfter {

  after {
    DateTimeUtils.setCurrentMillisSystem() // put the system clock back to normal
  }

  implicit lazy val cacheExecutor = {
    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("cache-%d").build()
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(namedThreadFactory))
  }

  case class CacheStuff(cache: Memcached, latestVersionCache: LatestVersionCache, client: MemcachedClient)

  def createCacheStuff: CacheStuff = {
    val cache = new InMemoryCacheAdapter()
    val cacheAccessor = new MemcachedAccessor(cache, MemcachedKeyGenerator)
    val latestVersionCache = new InMemoryLatestVersionCache
    val client = new MemcachedClient(cacheAccessor, latestVersionCache)
    CacheStuff(cache, latestVersionCache, client)
  }

  it("should miss but insert new part version when cache is empty") {
    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id", Nil, Map.empty, Some(5 hours))
    val fresp = testee.putIfAbsent(cacheDirective) {
      Future.failed(new IOException)
    }
    whenReady(fresp.failed) {
      t: Throwable =>
        t shouldBe a[java.io.IOException]
        eventually {
          // because the version is written to memcached asynchronously
          whenReady(testee.PartVersionCache(cacheDirective.partId).pollVersion) {
            ov =>
              ov shouldNot be(None)
              ov.get shouldBe >(0L)
          }
        }
    }
  }

  it("should update the LVC with the latest part version from the external cache") {
    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id 2", Nil, Map.empty, Some(5 hours))
    val version = 13L
    val iext = testee.PartVersionCache(cacheDirective.partId).doInsertExternal(version)
    whenReady(iext) {
      _ =>
        val fresp = testee.putIfAbsent(cacheDirective) {
          Future.failed(new IOException)
        }
        whenReady(fresp.failed) {
          t: Throwable =>
            t shouldBe a[java.io.IOException]
            cacheStuff.latestVersionCache.getPartVersion(cacheDirective.partId) should be(Some(version))
        }
    }
  }

  it("should store the response on success, then reuse it") {
    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id 3", Nil, Map.empty, Some(5 hours))
    val version = 13L
    val iext = testee.PartVersionCache(cacheDirective.partId).doInsertExternal(version)
    whenReady(iext) {
      _ =>
        cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
        val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective, testee.CombinedVersionLookup.knownVersions(cacheDirective)).get
        val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId)
        val cacheMissResp = testee.putIfAbsent(cacheDirective) {
          Future.successful(partResponse)
        }
        whenReady(cacheMissResp) {
          resp =>
            resp should be(partResponse)
            eventually {
              // because the PartResponse is written to memcached asynchronously
              whenReady(testee.PartResponseCache.pollPartResponse(cacheKey)) {
                _ should be(Some(partResponse))
              }
            }
            val cacheHitResp = testee.putIfAbsent(cacheDirective) {
              throw new AssertionError // because this block would only be called on a cache miss
            }
            whenReady(cacheHitResp) {
              _ should be(partResponse.copy(retrievedFromCache = true))
            }
        }
    }
  }

  it("should regenerate a version on memcached version cache expiry") {
    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client

    // 1. Generate a versioned cache key for a cache directive
    val cacheDirective = CacheDirective("some part id 4", Nil, Map.empty, Some(5 hours))
    val version = 13L
    cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
    val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective).get
    val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId)

    // 2. insert only the value into memcached (forget the version)
    testee.PartResponseCache.insertPartResponse(cacheKey, partResponse, cacheDirective.ttl)

    // 3. Generate a versioned cache key for the same cached directive and verify that the versioning is NOT the same as what was generated in 1.
    val fresp = testee.putIfAbsent(cacheDirective) {
      Future.failed(new IOException)
    }
    whenReady(fresp.failed) {
      e: Throwable =>
        e shouldBe a[IOException]
        eventually {
          // because the version is written to memcached asynchronously
          whenReady(testee.PartVersionCache(cacheDirective.partId).pollVersion) {
            optV =>
              optV should not be None
              optV.get should not be cacheKey.versions.head
          }
        }
    }
  }

  it("should show a CacheException when the cache is down") {
    val cacheStuff = createCacheStuff
    val memcached = new InMemoryCacheAdapter() {
      override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = Future.failed(new IOException)
    }
    val memcachedAccessor = new MemcachedAccessor(memcached, MemcachedKeyGenerator)
    val testee = new MemcachedClient(memcachedAccessor, cacheStuff.latestVersionCache)
    val version = 13L
    val cacheDirective = CacheDirective("some part id 5", Nil, Map.empty, Some(5 hours))
    cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
    val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective)
    val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId)
    val fresp = testee.putIfAbsent(cacheDirective)(Future.successful(partResponse))
    whenReady(fresp.failed) {
      e: Throwable =>
        e shouldBe a[CacheException]
        e.getCause shouldBe a[IOException]
        e.asInstanceOf[CacheException].key should be(cacheKey.get)
    }
  }

  describe("when a value was retrieved from the cache but the versions in the LVC do not match those in memcached") {
    it("should try another cache lookup using the versions retrieved from memcached") {
      val cacheStuff = createCacheStuff
      val testee = cacheStuff.client

      val cacheDirective = CacheDirective("some part id 6", Nil, Map.empty, Some(5 hours))
      val stalePartResponse = PartResponse(cacheDirective.partId, cacheDirective.partId, contents = Some("I'm from the cache but I'm old!"), retrievedFromCache = true)
      val freshPartResponse = PartResponse(cacheDirective.partId, cacheDirective.partId, contents = Some("I'm from the cache and I'm fresh!"), retrievedFromCache = true)
      val retrievedByHttp = PartResponse(cacheDirective.partId, cacheDirective.partId, contents = Some("I was retrieved via HTTP!"))

      // Set up versions: 10 in memory, 20 in memcached
      val internalVersion = 10L
      val externalVersion = 20L
      cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, internalVersion)
      testee.PartVersionCache(cacheDirective.partId).doInsertExternal(externalVersion)

      // Insert both the stale (version 10) and fresh (version 20) PartResponses into memcached
      val partKeyVer10 = PartCacheKey(cacheDirective.partId, List(internalVersion), Map.empty)
      val partKeyVer20 = PartCacheKey(cacheDirective.partId, List(externalVersion), Map.empty)
      testee.PartResponseCache.insertPartResponse(partKeyVer10, stalePartResponse, Some(5 hours))
      testee.PartResponseCache.insertPartResponse(partKeyVer20, freshPartResponse, Some(5 hours))

      // Should lookup with version 10, miss, then lookup successfully with version 20
      val fResp = testee.putIfAbsent(cacheDirective)(Future.successful(retrievedByHttp))
      whenReady(fResp) {
        resp: PartResponse =>
          resp should be(freshPartResponse)
      }
    }
  }

  describe("cache miss") {
    it("should NOT set the retrievedFromCache flag on the response") {
      val cacheStuff = createCacheStuff
      val testee = cacheStuff.client
      val cacheDirective = CacheDirective("some part id 7", Nil, Map.empty, Some(5 hours))
      val cacheMissResp = testee.putIfAbsent(cacheDirective) {
        Future.successful(PartResponse(cacheDirective.partId, cacheDirective.partId))
      }
      whenReady(cacheMissResp) {
        _.retrievedFromCache shouldBe false
      }
    }
  }

  describe("cache hit") {
    it("should set the retrievedFromCache flag on the response") {
      val cacheStuff = createCacheStuff
      val testee = cacheStuff.client
      val cacheDirective = CacheDirective("some part id 8", Nil, Map.empty, Some(5 hours))
      val cacheMissResp = testee.putIfAbsent(cacheDirective) {
        Future.successful(PartResponse(cacheDirective.partId, cacheDirective.partId))
      }
      whenReady(cacheMissResp) {
        _ =>
          // Value will be written to cache asynchronously, but eventually we should get a cache hit
          eventually {
            val fResp = testee.putIfAbsent(cacheDirective) {
              Future.successful(PartResponse(cacheDirective.partId, cacheDirective.partId))
            }
            whenReady(fResp) {
              _.retrievedFromCache should be(true)
            }
          }
      }
    }
  }

  it("should not cache a PartResponse that has the noStore flag set") {
    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id 9", Nil, Map.empty, Some(5 hours))
    val version = 13L
    val iext = testee.PartVersionCache(cacheDirective.partId).doInsertExternal(version)
    whenReady(iext) {
      _ =>
        cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
        val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective, testee.CombinedVersionLookup.knownVersions(cacheDirective)).get
        val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId, cacheControl = CacheControl(noStore = true))
        val cacheMissResp = testee.putIfAbsent(cacheDirective) {
          Future.successful(partResponse)
        }
        whenReady(cacheMissResp) {
          resp =>
            resp should be(partResponse)
            Thread.sleep(200L) // wait for a while because cache write happens asynchronously
            whenReady(testee.PartResponseCache.pollPartResponse(cacheKey)) {
              _ should be(None) // it should not have written the response to the cache
            }
        }
    }
  }

  it("should not cache a PartResponse that has expired") {
    val now = 1401148800000L
    DateTimeUtils.setCurrentMillisFixed(now)

    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id 10", Nil, Map.empty, Some(5 hours))
    val version = 13L
    val iext = testee.PartVersionCache(cacheDirective.partId).doInsertExternal(version)
    whenReady(iext) {
      _ =>
        cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
        val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective, testee.CombinedVersionLookup.knownVersions(cacheDirective)).get
        val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId, cacheControl = CacheControl(expiresAt = Some(now - 1L))) // expired 1ms ago
        val cacheMissResp = testee.putIfAbsent(cacheDirective) {
          Future.successful(partResponse)
        }
        whenReady(cacheMissResp) {
          resp =>
            resp should be(partResponse)
            Thread.sleep(200L) // wait for a while because cache write happens asynchronously
            whenReady(testee.PartResponseCache.pollPartResponse(cacheKey)) {
              _ should be(None) // it should not have written the response to the cache
            }
        }
    }
  }

  it("should not cache a PartResponse that is expiring right now") {
    val now = 1401148800000L
    DateTimeUtils.setCurrentMillisFixed(now)

    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id 11", Nil, Map.empty, Some(5 hours))
    val version = 13L
    val iext = testee.PartVersionCache(cacheDirective.partId).doInsertExternal(version)
    whenReady(iext) {
      _ =>
        cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
        val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective, testee.CombinedVersionLookup.knownVersions(cacheDirective)).get
        val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId, cacheControl = CacheControl(expiresAt = Some(now))) // expiring exactly now
        val cacheMissResp = testee.putIfAbsent(cacheDirective) {
          Future.successful(partResponse)
        }
        whenReady(cacheMissResp) {
          resp =>
            resp should be(partResponse)
            Thread.sleep(200L) // wait for a while because cache write happens asynchronously
            whenReady(testee.PartResponseCache.pollPartResponse(cacheKey)) {
              _ should be(None) // it should not have written the response to the cache
            }
        }
    }
  }

  it("should cache a PartResponse that has not yet expired") {
    val now = 1401148800000L
    DateTimeUtils.setCurrentMillisFixed(now)

    val cacheStuff = createCacheStuff
    val testee = cacheStuff.client
    val cacheDirective = CacheDirective("some part id 12", Nil, Map.empty, Some(5 hours))
    val version = 13L
    val iext = testee.PartVersionCache(cacheDirective.partId).doInsertExternal(version)
    whenReady(iext) {
      _ =>
        cacheStuff.latestVersionCache.updatePartVersion(cacheDirective.partId, version)
        val cacheKey = testee.PartCacheKeyFactory.tryApply(cacheDirective, testee.CombinedVersionLookup.knownVersions(cacheDirective)).get
        val partResponse = PartResponse(cacheDirective.partId, cacheDirective.partId, cacheControl = CacheControl(expiresAt = Some(now + 1000L))) // expiring 1s from now
        val cacheMissResp = testee.putIfAbsent(cacheDirective) {
          Future.successful(partResponse)
        }
        whenReady(cacheMissResp) {
          resp =>
            resp should be(partResponse)
            eventually {
              // Value will be written to cache asynchronously, but eventually we should get a cache hit
              whenReady(testee.PartResponseCache.pollPartResponse(cacheKey)) {
                _ should be(Some(partResponse)) // it should have cached the response
              }
            }
        }
    }
  }

}
