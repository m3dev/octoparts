package com.m3.octoparts.http

import com.m3.octoparts.OctopartsMetricsRegistry
import org.apache.http.client.methods.HttpHead
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.duration._

class BasicHttpClientSpec extends FunSpec with Matchers {
  it("should be able to create several instrumented clients") {
    val clients = for (i <- 1 to 30) yield new BasicHttpClient(i.toString, connectTimeout = i.seconds)
    try {
      clients.distinct should have size clients.size
    } finally {
      clients.foreach(_.close())
    }
  }

  it("should not fail (only warn) if 2 clients share the same name") {
    val clients = for (i <- 1 to 2) yield new BasicHttpClient("A")
    try {
      clients.distinct should have size clients.size
    } finally {
      clients.foreach(_.close())
    }
  }

  it("should be able to close and recreate a client") {
    {
      val client = new BasicHttpClient("A")
      client.close()
      BasicHttpClient.gauges.keys.foreach { key =>
        OctopartsMetricsRegistry.default.getGauges.get(client.registryName(key)) should be(null)
      }
    }
    {
      val client = new BasicHttpClient("A")
      try {
        client.retrieve(new HttpHead("http://example.com/")).status should be(200)
        BasicHttpClient.gauges.keys.foreach { key =>
          OctopartsMetricsRegistry.default.getGauges.get(client.registryName(key)) shouldNot be(null)
        }
      } finally {
        client.close()
      }
    }
  }
}
