package com.m3.octoparts.model.config

import java.nio.charset.StandardCharsets

import shade.memcached.Codec
import shade.memcached.MemcachedCodecs.AnyRefBinaryCodec

/**
 * Created by Lloyd on 9/4/14.
 */
object CacheCodecs {

  implicit def optionCodecFor[A: Codec]: Codec[Option[A]] = new Codec[Option[A]] {
    def serialize(value: Option[A]): Array[Byte] = value.fold("None".getBytes(StandardCharsets.UTF_8)) { v =>
      implicitly[Codec[A]].serialize(v)
    }

    def deserialize(data: Array[Byte]): Option[A] = if (new String(data, StandardCharsets.UTF_8) == "None") {
      None
    } else {
      Some(implicitly[Codec[A]].deserialize(data))
    }
  }

  implicit val cacheGroupCodec = AnyRefBinaryCodec[CacheGroup]
  implicit val httpPartConfigCodec = AnyRefBinaryCodec[HttpPartConfig]
  implicit val httpPartConfigCacheGroupCodec = AnyRefBinaryCodec[HttpPartConfigCacheGroup]
  implicit val hystrixConfigCodec = AnyRefBinaryCodec[HystrixConfig]
  implicit val partParamCodec = AnyRefBinaryCodec[PartParam]
  implicit val partParamCacheGroupCodec = AnyRefBinaryCodec[PartParamCacheGroup]
  implicit val shortPartParamCodec = AnyRefBinaryCodec[ShortPartParam]
  implicit val threadPoolConfigCodec = AnyRefBinaryCodec[ThreadPoolConfig]

}
