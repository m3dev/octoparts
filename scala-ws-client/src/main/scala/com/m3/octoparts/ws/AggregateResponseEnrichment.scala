package com.m3.octoparts.ws

import java.io.IOException

import com.m3.octoparts.model.{ AggregateResponse, PartResponse }
import org.apache.commons.lang3.{ StringUtils, SystemUtils }
import play.api.Logger
import play.api.libs.json.JsonValidationError
import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

object AggregateResponseEnrichment {

  case class OctopartsException(
    partResponse: PartResponse
  ) extends RuntimeException(partResponse.errors.mkString(SystemUtils.LINE_SEPARATOR))

  private val logger = Logger("com.m3.octoparts.AggregateResponseEnrichment")

  implicit class RichPartResponse(val part: PartResponse) extends AnyVal {

    /**
     * @return The partId and the request-specific id. Used to uniquely identify parts when printing messages.
     */
    def fullId: String = {
      if (part.id == part.partId) part.id
      else s"${part.partId} (${part.id})"
    }

    /**
     * @return [[PartResponse.contents]] only if there is some non-blank contents.
     */
    def tryContents: Try[String] = {
      part.contents match {
        case Some(contents) if StringUtils.isNotBlank(contents) => Success(contents)
        case _ => Failure(new IOException("No content"))
      }
    }

    /**
     * Prints all parts warnings (deprecation...)
     */
    def printWarnings(): Unit = for (warning <- part.warnings) {
      logger.warn(s"In part $fullId: $warning")
    }

    /**
     * Prints [[PartResponse.warnings]], and try to extract [[PartResponse.contents]] only if there were no [[PartResponse.errors]] .
     * @return
     */
    def tryContentsIfNoError: Try[String] = {
      printWarnings()
      if (part.errors.isEmpty) tryContents else Failure(OctopartsException(part))
    }

    /**
     * Will return a failure if
     * - the part has no content
     * - the JSON parsing fails (e.g. the content is not JSON, or the JSON is broken in some way)
     * - the JSON deserialization fails (i.e. the JSON is valid, but cannot be deserialized into an [[A]])
     *
     * @tparam A the result type, i.e. the type of the JSON-serialized object
     */
    def tryJson[A: Reads]: Try[A] = {
      for {
        contents <- tryContents
        json <- Try(Json.parse(contents))
        a <- mapJson(json)
      } yield {
        a
      }
    }

    /**
     * Same as [[tryJson]] but only tries it when there is no [[PartResponse.errors]]
     */
    def tryJsonIfNoError[A: Reads]: Try[A] = {
      for {
        contents <- tryContentsIfNoError
        json <- Try(Json.parse(contents))
        a <- mapJson(json)
      } yield {
        a
      }
    }
  }

  /**
   * Convenience methods to make it easier to work with [[AggregateResponse]]
   */
  implicit class RichAggregateResponse(
      val aggResp: AggregateResponse
  ) extends AnyVal {

    /**
     * Extracts the part with the given ID from the Octoparts response and deserializes its JSON content to an [[A]].
     *
     * Note that an appropriate [[play.api.libs.json.Reads]] must be available in implicit scope.
     *
     * Will return a Failure if:
     * - a part with the given ID is not present in the response
     * - the part has no content
     * - the JSON parsing fails (e.g. the content is not JSON, or the JSON is broken in some way)
     * - the JSON deserialization fails (i.e. the JSON is valid, but cannot be deserialized into an [[A]])
     *
     * Note:
     * - The method does not check the status code of the response or the presence of error messages.
     *
     * @param id the part request unique id (or partId if the part request did not specify an ID)
     * @tparam A the result type, i.e. the type of the JSON-serialized object
     * @return the object, or Failure if it could not be found and deserialized for some reason.
     */
    def tryJsonPart[A: Reads](id: String): Try[A] = tryFindPart(id).flatMap(_.tryJsonIfNoError)

    /**
     * Logs any errors from [[tryJsonPart]] and collapses to an Option
     *
     * @param id
     * @tparam A
     * @return
     */
    def getJsonPart[A: Reads](id: String): Option[A] = tryJsonPart(id) match {
      case Success(a) => Some(a)
      case Failure(failure) =>
        logger.warn(s"Object not retrievable from part response: $id", failure)
        failure match {
          case OctopartsException(pr) => logger.debug(s"Part response: $pr")
          case _ =>
        }
        None
    }

    def getJsonPartOrElse[A: Reads](id: String, default: => A): A =
      getJsonPart(id).getOrElse(default)

    /**
     * Deserializes like [[tryJsonPart]],
     * but will try instead to deserialize to an [[E]]
     * if the part response contained errors
     * @tparam A
     * @tparam E
     */
    def getJsonPartOrError[A: Reads, E: Reads](
      id: String
    ): Either[Option[E], A] = tryFindPart(id) match {
      case Failure(noPart) => {
        logger.warn("Could not retrieve object from response", noPart)
        Left(None)
      }
      case Success(part) => part.tryJsonIfNoError[A] match {
        case Success(a) => Right(a)
        case Failure(_) => {
          part.tryJson[E] match {
            case Success(e) => Left(Some(e))
            case Failure(failureE) =>
              logger.warn("Could not deserialize error", failureE)
              Left(None)
          }
        }
      }
    }

    def tryFindPart(id: String): Try[PartResponse] = {
      aggResp.findPart(id) match {
        case None => Failure(new NoSuchElementException(s"part with id=$id not found"))
        case Some(part) => Success(part)
      }
    }
  }

  private def mapJson[A: Reads](json: JsValue): Try[A] = {
    val jsResult = Json.fromJson[A](json)
    jsResult.fold(
      invalid => Failure(new IOException(s"Invalid JSON: $jsResult, ${jsonErrorMsg(invalid)}")),
      Success.apply
    )
  }

  private def jsonErrorMsg(errors: Seq[(JsPath, Seq[JsonValidationError])]): String = {
    errors.map {
      case (jsPath, validationErrors) => {
        val validationErrorMsg = validationErrors.map(_.message).mkString(",")
        s"error at: $jsPath reason: $validationErrorMsg"
      }
    }.mkString("; ")
  }
}
