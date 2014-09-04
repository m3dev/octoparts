package com.m3.octoparts.model.config

import java.nio.charset.StandardCharsets

import shade.memcached.Codec
import shade.memcached.MemcachedCodecs.AnyRefBinaryCodec

object CacheCodecs {

  private val NoneStringBytes: Array[Byte] = None.toString.getBytes(StandardCharsets.UTF_8)

  implicit def optionCodecFor[A: Codec]: Codec[Option[A]] = new Codec[Option[A]] {
    def serialize(value: Option[A]): Array[Byte] =
      value.fold(NoneStringBytes)(implicitly[Codec[A]].serialize)

    def deserialize(data: Array[Byte]): Option[A] =
      if (data == NoneStringBytes) None else Some(implicitly[Codec[A]].deserialize(data))
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
