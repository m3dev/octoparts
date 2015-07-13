package controllers

import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{ RequestHeader, Action }

class ApiHelpController extends SwaggerBaseApiController {

  def getResources = Action {
    request =>
      implicit val requestHeader: RequestHeader = request

      val resourceListing = getResourceListing

      val responseStr = returnXml(request) match {
        case true => toXmlString(resourceListing)
        case false => toJsonString(resourceListing)
      }
      returnValue(request, responseStr)
  }

  def getResource(path: String) = Action {
    request =>
      implicit val requestHeader: RequestHeader = request

      val apiListing = getApiListing(path)

      val responseStr = returnXml(request) match {
        case true => toXmlString(apiListing)
        case false => toJsonString(apiListing)
      }
      Option(responseStr) match {
        case Some(help) => returnValue(request, help)
        case None =>
          val msg = new ErrorResponse(500, "api listing for path " + path + " not found")
          Logger("swagger").error(msg.message)
          if (returnXml(request)) {
            InternalServerError.chunked(Enumerator(toXmlString(msg).getBytes)).as("application/xml")
          } else {
            InternalServerError.chunked(Enumerator(toJsonString(msg).getBytes)).as("application/json")
          }
      }
  }
}