package com.m3.octoparts.ws

import java.io.IOException

import com.m3.octoparts.model.{ AggregateResponse, PartResponse }
import org.apache.commons.lang3.StringUtils
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json.{ JsPath, JsValue, Json, Reads }

import scala.util.{ Try, Success, Failure }

object AggregateResponseEnrichment {

  private val logger = Logger("com.m3.octoparts.AggregateResponseEnrichment")

  /**
   * Convenience methods to make it easier to work with [[com.m3.octoparts.model.AggregateResponse]]
   */
  implicit class RichAggregateResponse(val aggResp: AggregateResponse) extends AnyVal {

    /**
     * Extracts the part with the given ID from the Octoparts response and deserializes its JSON content to an [[A]].
     *
     * Note that an appropriate [[play.api.libs.json.Reads]] must be available in implicit scope.
     *
     * Will return None if:
     * - a part with the given ID is not present in the response
     * - the part has no content
     * - the JSON parsing fails (e.g. the content is not JSON, or the JSON is broken in some way)
     * - the JSON deserialization fails (i.e. the JSON is valid, but cannot be deserialized into an [[A]])
     *
     * Note:
     * - The method does not check the status code of the response or the presence of error messages.
     *
     * @param id the part request unique id (or partId if the part request did not specify an ID)
     * @param recoverWith can be customized to return something even if JSON extraction failed.
     * @tparam A the result type, i.e. the type of the JSON-serialized object
     * @return the object, or None if it could not be found and deserialized for some reason.
     */
    def getJsonPart[A: Reads](id: String, recoverWith: (String, Throwable) => Option[A] = warnFailure): Option[A] = {
      tryJsonPart[A](id) match {
        case Failure(e) => recoverWith(id, e)
        case Success(v) => Some(v)
      }
    }

    def getJsonPartOrElse[A: Reads](id: String, default: => A): A = getJsonPart[A](id).getOrElse(default)

    private def tryJsonPart[A: Reads](id: String): Try[A] = {
      for {
        part <- findPart(id)
        contents <- getContents(id, part)
        json <- Try(Json.parse(contents))
        a <- mapJson(json)
      } yield {
        a
      }
    }

    private def findPart(id: String): Try[PartResponse] = {
      aggResp.findPart(id).fold[Try[PartResponse]] {
        Failure(new IllegalArgumentException(s"part with id=$id not found"))
      }(Success.apply)
    }
  }

  private def getContents(id: String, part: PartResponse): Try[String] = {
    part.contents match {
      case Some(contents) if StringUtils.isNotBlank(contents) => {
        // print remaining errors
        printErrors(id, part)
        Success(contents)
      }
      case _ => Failure(new IOException(part.errors.headOption.getOrElse("No content")))
    }
  }

  private def printErrors(id: String, part: PartResponse): Unit = {
    for {
      error <- part.errors
    } {
      logger.warn(s"Error in part with id: $id, error: $error")
    }
  }

  private def mapJson[A: Reads](json: JsValue): Try[A] = {
    val jsResult = Json.fromJson[A](json)
    jsResult.fold(
      invalid => Failure(new IOException(s"Invalid JSON: $jsResult, ${jsonErrorMsg(invalid)}")),
      Success.apply
    )
  }

  private def jsonErrorMsg(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map {
      case (jsPath, validationErrors) => {
        val validationErrorMsg = validationErrors.map { validationError =>
          Messages(validationError.message, validationError.args)
        }.mkString(",")
        s"error at: $jsPath reason: $validationErrorMsg"
      }
    }.mkString("; ")
  }

  val warnFailure: (String, Throwable) => Option[Nothing] = {
    (id, failure) =>
      logger.warn(s"Object not retrievable from part response: $id", failure)
      None
  }
}
