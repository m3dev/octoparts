package com.m3.octoparts.http

import com.m3.octoparts.OctopartsMetricsRegistry
import org.apache.http.client.methods.HttpHead
import org.scalatest.{FunSpec, Matchers}

class InstrumentedHttpClientHttpClientSpec extends FunSpec with Matchers {
  it("should not fail (only warn) if 2 clients share the same name") {
    val clients = for (i <- 1 to 2) yield new InstrumentedHttpClient("A")
    try {
      clients.foreach {
        _.retrieve(new HttpHead("http://example.com/")).status should be(200)
      }
    } finally {
      clients.foreach(_.close())
    }
  }

  it("should remove gauges on close") {
    val client = new InstrumentedHttpClient("A")
    InstrumentedHttpClient.gauges.keys.foreach { key =>
      OctopartsMetricsRegistry.default.getGauges.get(client.registryName(key)) shouldNot be(null)
    }
    client.close()
    InstrumentedHttpClient.gauges.keys.foreach { key =>
      OctopartsMetricsRegistry.default.getGauges.get(client.registryName(key)) should be(null)
    }
  }

  it("should be able to close and recreate a client with the same name") {
    new InstrumentedHttpClient("A").close()
    val client = new InstrumentedHttpClient("A")
    try {
      client.retrieve(new HttpHead("http://example.com/")).status should be(200)
    } finally {
      client.close()
    }

    it("should set the connection pool size") {
      val client = new InstrumentedHttpClient("A", connectionPoolSize = 42)
      try {
        client.connectionManager.getMaxTotal should be(42)
        client.connectionManager.getDefaultMaxPerRoute should be(42)
      } finally {
        client.close()
      }
    }
  }
}
