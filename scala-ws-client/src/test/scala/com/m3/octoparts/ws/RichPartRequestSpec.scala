package com.m3.octoparts.ws

import java.nio.charset.StandardCharsets

import akka.util.ByteString
import com.m3.octoparts.model.{ PartRequest, PartRequestParam }
import org.scalatest._
import play.api.http.Writeable

class RichPartRequestSpec extends FunSpec with Matchers {

  describe("#withBody") {

    import PartRequestEnrichment._
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val tuple3IntWriteable = new Writeable[(Int, Int, Int)](
      transform = { a => ByteString(s"${a._1 + a._2 + a._3}".getBytes(StandardCharsets.UTF_8)) }, None
    )

    it("should add a PartRequestParam with a key of 'body' and a value of whatever the implicit Writeable.transform returns as a string") {
      val pr = PartRequest("hello")
      val prWithBody = pr.withBody((1, 2, 3))
      prWithBody.params.find(_.key == "body").head.value shouldBe "6"
    }

    it("should ensure that there is only 1 'body' param in the part request; the one it adds") {
      val pr = PartRequest("hello", params = Seq(PartRequestParam("body", "booo")))
      val prWithBody = pr.withBody((9, 10, 11))
      prWithBody.params.count(_.key == "body") shouldBe 1
      prWithBody.params.find(_.key == "body").head.value shouldBe "30"
    }

  }

}
