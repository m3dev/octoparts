package com.m3.octoparts.client

import com.fasterxml.jackson.core._
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.concurrent.duration._

private[client] trait DurationModule extends JacksonModule {

  this += DurationSerializerResolver
  this += DurationDeserializerResolver

  private object DurationSerializerResolver extends Serializers.Base {

    override def findSerializer(config: SerializationConfig,
                                javaType: JavaType,
                                beanDescription: BeanDescription): JsonSerializer[_] = {

      if (classOf[Duration].isAssignableFrom(javaType.getRawClass)) {
        DurationSerializer
      } else {
        null
      }
    }

    object DurationSerializer extends JsonSerializer[Duration] {
      override def serialize(value: Duration, jgen: JsonGenerator, provider: SerializerProvider) = {
        jgen.writeNumber(value.toMillis)
      }
    }
  }

  private object DurationDeserializerResolver extends Deserializers.Base {

    override def findBeanDeserializer(javaType: JavaType,
                                      config: DeserializationConfig,
                                      beanDesc: BeanDescription) = {

      if (classOf[Duration].isAssignableFrom(javaType.getRawClass)) {
        new DurationDeserializer(javaType)
      } else {
        null
      }
    }

    class DurationDeserializer(javaType: JavaType) extends JsonDeserializer[Duration] {
      override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Duration = {
        if (jp.getCurrentToken != JsonToken.VALUE_NUMBER_INT)
          throw ctxt.mappingException(javaType.getRawClass)
        jp.getLongValue.millis
      }
    }

  }

}
