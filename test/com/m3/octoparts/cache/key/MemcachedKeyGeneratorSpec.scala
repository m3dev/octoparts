package com.m3.octoparts.cache.key

import org.scalatest.FunSpec
import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary

class MemcachedKeyGeneratorSpec extends FunSpec with Checkers {
  implicit val cacheKeyGen: Arbitrary[CacheKey] = Arbitrary({
    for (bytes <- implicitly[Arbitrary[Array[Byte]]].arbitrary)
      yield new CacheKey {
      val someData = bytes
    }
  })
  it("should not produce empty input") {
    check({
      (cacheKey: CacheKey) =>
        MemcachedKeyGenerator.toMemcachedKey(cacheKey).length > 0
    }, MaxSize(5))
  }

  it("should not output something longer than 250") {
    check({
      (cacheKey: CacheKey) =>
        MemcachedKeyGenerator.toMemcachedKey(cacheKey).length <= MemcachedKeyGenerator.maxKeyLength
    }, MaxSize(5000), MinSuccessful(100))
  }

  it("should not contain any invalid character") {
    val memcachedInvalid = "[^\\x21-\\x7e]".r
    check({
      (cacheKey: CacheKey) =>
        val genKey = MemcachedKeyGenerator.toMemcachedKey(cacheKey)
        memcachedInvalid.findFirstIn(genKey).isEmpty
    }, MinSuccessful(100))
  }

  // Commented out because it runs too slowly
  //  it("should not produce hash collisions") {
  //    check({
  //      (cacheKey1: CacheKey, cacheKey2: CacheKey) =>
  //        val genKey1 = MemcachedKeyGenerator.toMemcachedKey(cacheKey1)
  //        val genKey2 = MemcachedKeyGenerator.toMemcachedKey(cacheKey2)
  //        (genKey1 == genKey2) == (cacheKey1 == cacheKey2)
  //    }, MinSuccessful(10000))
  //  }
}
