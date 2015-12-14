package com.m3.octoparts.http

import java.nio.charset.StandardCharsets

import com.m3.octoparts.support.MetricsSupport
import org.apache.http.client.methods.HttpHead
import org.scalatest.{ FunSpec, Matchers }
import scala.concurrent.duration._

class InstrumentedHttpClientSpec extends FunSpec with Matchers with MetricsSupport {

  it("should not fail (only warn) if 2 clients share the same name") {
    val clients = for (i <- 1 to 2) yield {
      new InstrumentedHttpClient(
        name = "A",
        connectionPoolSize = 1,
        connectTimeout = 5.seconds,
        socketTimeout = 5.seconds,
        defaultEncoding = StandardCharsets.UTF_8,
        mbProxySettings = None,
        metrics = metrics
      )
    }
    try {
      clients.foreach {
        _.retrieve(new HttpHead("http://example.com/")).status should be(200)
      }
    } finally {
      clients.foreach(_.close())
    }
  }

  it("should remove gauges on close") {
    val client = new InstrumentedHttpClient(
      name = "A",
      connectionPoolSize = 1,
      connectTimeout = 5.seconds,
      socketTimeout = 5.seconds,
      defaultEncoding = StandardCharsets.UTF_8,
      mbProxySettings = None,
      metrics = metrics
    )
    InstrumentedHttpClient.gauges.keys.foreach { key =>
      metrics.defaultRegistry.getGauges.get(client.connectionManager.registryName(key)) should not be null
    }
    client.close()
    InstrumentedHttpClient.gauges.keys.foreach { key =>
      metrics.defaultRegistry.getGauges.get(client.connectionManager.registryName(key)) shouldBe null
    }
  }

  it("should be able to close and recreate a client with the same name") {
    new InstrumentedHttpClient(
      name = "A",
      connectionPoolSize = 1,
      connectTimeout = 5.seconds,
      socketTimeout = 5.seconds,
      defaultEncoding = StandardCharsets.UTF_8,
      mbProxySettings = None,
      metrics = metrics
    ).close()
    val client = new InstrumentedHttpClient(
      name = "A",
      connectionPoolSize = 1,
      connectTimeout = 5.seconds,
      socketTimeout = 5.seconds,
      defaultEncoding = StandardCharsets.UTF_8,
      mbProxySettings = None,
      metrics = metrics
    )
    try {
      client.retrieve(new HttpHead("http://example.com/")).status should be(200)
    } finally {
      client.close()
    }
  }

  it("should set the connection pool size") {
    val client = new InstrumentedHttpClient(
      name = "A",
      connectionPoolSize = 42,
      connectTimeout = 5.seconds,
      socketTimeout = 5.seconds,
      defaultEncoding = StandardCharsets.UTF_8,
      mbProxySettings = None,
      metrics = metrics
    )
    try {
      client.connectionManager.getMaxTotal should be(42)
      client.connectionManager.getDefaultMaxPerRoute should be(42)
    } finally {
      client.close()
    }
  }
}
