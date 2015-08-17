package com.m3.octoparts.http

import java.net.URI
import org.apache.http.HttpEntityEnclosingRequest
import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.concurrent.ScalaFutures
import com.m3.octoparts.model.HttpMethod._
import org.apache.http.client.methods._
import com.m3.octoparts.hystrix.MockHttpClientComponent

class BlockingHttpRetrieveSpec extends FunSpec with Matchers with ScalaFutures {

  describe("BlockingHttpRetrieve") {
    trait HttpRetrieveBodyContext {
      def httpMethod: HttpMethod
      def uriForCommand: URI = new URI("http://beachape.com")
      def command = new BlockingHttpRetrieve with MockHttpClientComponent {
        val method = httpMethod
        val maybeBody = Some("thing")
        val headers = Nil
        val uri = uriForCommand
      }
    }
    describe("#request") {
      it("should return the right HttpUriRequest for Get") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Get
          command.request shouldBe a[HttpGet]
        }
      }
      it("should return the right HttpUriRequest for Head") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Head
          command.request shouldBe a[HttpHead]
        }
      }
      it("should return the right HttpUriRequest for Post") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Post
          command.request shouldBe a[HttpPost]
        }
      }
      it("should return the right HttpUriRequest for Put") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Put
          command.request shouldBe a[HttpPut]
        }
      }
      it("should return the right HttpUriRequest for Delete") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Delete
          command.request shouldBe a[HttpDelete]
        }
      }
      it("should return the right HttpUriRequest for Patch") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Patch
          command.request shouldBe a[HttpPatch]
        }
      }
      it("should return the right HttpUriRequest for Options") {
        new HttpRetrieveBodyContext {
          override def httpMethod: HttpMethod = Options
          command.request shouldBe a[HttpOptions]
        }
      }
      describe("when there is a body for the given request type") {

        it("should have UTF-8 entity encoding") {
          Seq(Post, Put, Patch).foreach { method =>
            new HttpRetrieveBodyContext {
              override def httpMethod: HttpMethod = method
              command.request match {
                case req: HttpEntityEnclosingRequest => {
                  val entity = req.getEntity
                  entity.getContentType.getValue should include("charset=UTF-8")
                }
                case _ => fail("The request doesn'T contain entities")
              }
            }
          }
        }

      }
    }

  }

}
