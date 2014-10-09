package com.m3.octoparts.http

import org.scalatest.{ FunSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite

import scala.concurrent.duration._

class BasicHttpClientSpec extends FunSpec with Matchers with OneAppPerSuite {
  it("should be able to create several instrumented clients") {
    val clients = for (i <- 1 to 30) yield new BasicHttpClient(connectTimeout = i.seconds)
    try {
      clients.distinct should have size clients.size
    } finally {
      clients.foreach(_.close())
    }
  }
}
