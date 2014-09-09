package com.m3.octoparts.ws

import java.util.UUID

import com.m3.octoparts.model._
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Sample showing how to use the client.
 */
object Sample {
  import scala.concurrent.ExecutionContext.Implicits.global
  import play.api.Play.current

  // Define some dummy model classes
  case class UserProfile(id: Int, name: String)
  case class NewsArticle(id: Int, body: String)
  // and their JSON deserializers
  implicit val userProfileReads = Json.reads[UserProfile]
  implicit val newsArticleReads = Json.reads[NewsArticle]

  // Add some implicits to make the response easier to work with
  import AggregateResponseEnrichment._

  // Create a client
  val octoClient = new OctoClient("http://octoparts/", httpRequestTimeout = 1.second)

  // Build an AggregateRequest to send to Octoparts
  val aggregateRequest = AggregateRequest(
    requestMeta = RequestMeta(
      // Most of these fields are optional
      id = UUID.randomUUID().toString,
      serviceId = Some("frontend"),
      userId = Some("123"),
      requestUrl = Some("http://www.m3.com/foo"),
      timeout = Some(500.millis)
    ),
    requests = Seq(
      // A list of the endpoints you want to call, with parameters to send
      PartRequest(partId = "UserProfile", params = Set(PartRequestParam("uid", "123"))),
      PartRequest(partId = "LatestNews", params = Set(PartRequestParam("limit", "10")))
    )
  )

  // Send the request, get back a Future
  val fResp: Future[AggregateResponse] = octoClient.invoke(aggregateRequest)

  fResp.map { aggregateResponse =>

    // Result will be None if:
    // - Octoparts did not return a response for this endpoint (e.g. because the endpoint was too slow to respond)
    // - the response could not be deserialized into a UserProfile
    val userProfile: Option[UserProfile] = aggregateResponse.getJsonPart[UserProfile]("UserProfile")

    // Get the list of latest news articles, falling back to an empty list in case of error
    val latestNews: Seq[NewsArticle] = aggregateResponse.getJsonPartOrElse[Seq[NewsArticle]]("LatestNews", default = Nil)

    // Do stuff with the user profile and the list of news articles ...

  }

}
