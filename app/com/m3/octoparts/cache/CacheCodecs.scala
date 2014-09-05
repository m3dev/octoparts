package com.m3.octoparts.cache

import com.m3.octoparts.model.config._
import shade.memcached.Codec
import shade.memcached.MemcachedCodecs.AnyRefBinaryCodec
import java.util.Arrays

object CacheCodecs {

  private val NoneBytes: Array[Byte] = Array(0.toByte)
  private val SomeBytePrefix: Byte = 1

  implicit def optionCodecFor[A: Codec]: Codec[Option[A]] = new Codec[Option[A]] {
    def serialize(value: Option[A]): Array[Byte] =
      value.fold(NoneBytes)(SomeBytePrefix +: implicitly[Codec[A]].serialize(_))

    def deserialize(data: Array[Byte]): Option[A] =
      if (Arrays.equals(data, NoneBytes)) None else Some(implicitly[Codec[A]].deserialize(Arrays.copyOfRange(data, 1, data.length)))
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
