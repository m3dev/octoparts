package controllers.system

import com.typesafe.config.ConfigFactory
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._

class SystemConfigControllerSpec
    extends FlatSpec
    with Matchers
    with JsonCheckSupport
    with OneAppPerSuite {

  private def toLines(s: String): List[String] = s.trim.stripMargin.split(Array('\r', '\n')).toList.filter {
    _.nonEmpty
  }

  it should "pretty print the config as JSON" in {
    val config = ConfigFactory.parseString(
      """
        |foo.bar = "a"
        |foo.baz = 123
      """.stripMargin)
    val controller = new SystemConfigController(config)
    val result = controller.showSystemConfig.apply(FakeRequest())
    toLines(contentAsString(result)) should equal(toLines(
      """{
        |    "foo" : {
        |        "bar" : "a",
        |        "baz" : 123
        |    }
        |}"""))
  }

  it should "try its best to mask passwords" in {
    val config = ConfigFactory.parseString(
      """
        |foo.bar.password = "secret"
        |foo.baz.bing.hoge = "hello"
        |foo.baz.bing.password = "another secret"
        |foo.password.wow = "don't mask me!"
      """.stripMargin)
    val controller = new SystemConfigController(config)
    val result = controller.showSystemConfig.apply(FakeRequest())
    checkJson(result) {
      json =>
        (json \ "foo" \ "bar" \ "password").as[String] should be("****")
        (json \ "foo" \ "baz" \ "bing" \ "hoge").as[String] should be("hello")
        (json \ "foo" \ "baz" \ "bing" \ "password").as[String] should be("****")
        (json \ "foo" \ "password" \ "wow").as[String] should be("don't mask me!")
    }
  }

}
