package com.m3.octoparts.ws

import com.m3.octoparts.model.RequestMeta
import org.scalatest.{ Matchers, FunSpec }

class RequestMetaBuilderSpec extends FunSpec with Matchers {

  describe(".from") {
    it("should build a RequestMetaBuilder with a useable .apply") {
      val builder = RequestMetaBuilder.from[Int] { i =>
        RequestMeta(
          id = i.toString,
          userId = Some(i.toString),
          sessionId = Some(i.toString),
          requestUrl = Some("http://someUrl.com"),
          userAgent = Some("secret-agent-man"),
          timeoutMs = Some(i * 3001)
        )
      }
      val reqMeta = builder(3)
      reqMeta.id should be("3")
      reqMeta.userId should be(Some("3"))
      reqMeta.sessionId should be(Some("3"))
      reqMeta.requestUrl should be(Some("http://someUrl.com"))
      reqMeta.userAgent should be(Some("secret-agent-man"))
      reqMeta.timeoutMs should be(Some(9003L))
    }
  }

}
