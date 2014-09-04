package controllers.system

import play.api.libs.json.{ Reads, JsValue }
import play.api.mvc.Result
import play.api.test.Helpers._

import scala.concurrent.Future

trait JsonCheckSupport {

  def checkJson[A](result: Future[Result])(f: JsValue => A): A = {
    val json = contentAsJson(result)
    f(json)
  }

  def field[A: Reads](name: String)(implicit json: JsValue) = (json \ name).as[A]
  def string(name: String)(implicit json: JsValue) = field[String](name)
  def boolean(name: String)(implicit json: JsValue) = field[Boolean](name)

}
