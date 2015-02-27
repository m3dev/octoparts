package com.m3.octoparts.model.config

import org.scalatest.{ FunSpec, Matchers }

import scala.util.Success

class HttpProxySettingsSpec extends FunSpec with Matchers {

  it("should handle the specified valid test cases") {
    val validCases = Seq(
      "localhost" -> HttpProxySettings("http", "localhost", -1),
      "testserver01:8080" -> HttpProxySettings("http", "testserver01", 8080),
      "https://www.proxy.com" -> HttpProxySettings("https", "www.proxy.com", -1),
      "ssh://neighbour.wifi:22" -> HttpProxySettings("ssh", "neighbour.wifi", 22)
    )
    validCases.foreach {
      case (proxyStr, expect) =>
        withClue(proxyStr) {
          HttpProxySettings.parse(proxyStr) shouldBe Success(expect)
          expect.serialize shouldBe proxyStr
        }
    }
  }

  it("should fail on the specified invalid test cases") {
    val invalidCases = Seq(
      "",
      "host001:",
      "host001:Piraeus",
      "http:host001:8080",
      " host001:8080 "
    )
    invalidCases.foreach { invalidProxyStr =>
      withClue(invalidProxyStr) {
        HttpProxySettings.isValid(invalidProxyStr) shouldBe false
      }
    }
  }
}
