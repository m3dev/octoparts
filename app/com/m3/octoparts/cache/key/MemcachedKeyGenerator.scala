package com.m3.octoparts.cache.key

import java.util.Base64

import shade.memcached.MemcachedCodecs

import scala.collection.immutable.NumericRange.Inclusive

trait MemcachedKeyGenerator {

  /**
   * Convert the given cache key into a string suitable for use as a Memcached key.
   * This is done by calculating a hash of the key.
   */
  def toMemcachedKey(rawKey: CacheKey): String
}

object MemcachedKeyGenerator extends MemcachedKeyGenerator {
  val validRange: Inclusive[Char] = 0x21.toChar to 0x7e.toChar
  val maxKeyLength = 250

  // should cache this (thread local?) for efficiency
  private def md = java.security.MessageDigest.getInstance("SHA-256")

  private val serializer = MemcachedCodecs.AnyRefBinaryCodec[CacheKey]

  // expected length is 512/8=64
  private def digest(c: CacheKey): Array[Byte] = {
    md.digest(serializer.serialize(c))
  }

  // maps a byte array to base64 (will use 4 chars per 3 bytes)
  // it could be slightly optimized to use the whole validRange, e.g writing the whole hash in base ${validRange.length}
  private def mapToSafeRange(data: Array[Byte]): String = Base64.getEncoder.encodeToString(data)

  /**
   * Build a memcached-safe key by simply hashing the rawkey with sha-256
   */
  override def toMemcachedKey(cacheKey: CacheKey): String = {
    val hashedKey = mapToSafeRange(digest(cacheKey))
    val remainingSpace = maxKeyLength - hashedKey.length
    hashedKey + cacheKey.toString.take(remainingSpace).map {
      case ok if validRange.containsTyped(ok) => ok
      case invalid => '_'
    }
  }
}
