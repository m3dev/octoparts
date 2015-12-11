package com.m3.octoparts.ws

import java.util.UUID

import com.m3.octoparts.model._
import play.api.libs.json.Json
import play.api.libs.ws.WS

import scala.concurrent.Future
import scala.concurrent.duration._

import PartRequestEnrichment._

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
  val octoClient = new OctoClient(WS.client, "http://octoparts/", clientTimeout = 1.second)

  implicit object rmb extends RequestMetaBuilder[Int] {
    def apply(userId: Int) = RequestMeta(
      // Most of these fields are optional
      id = UUID.randomUUID().toString,
      serviceId = Some("frontend"),
      userId = Some(userId.toString),
      requestUrl = Some("http://www.m3.com/foo"),
      timeout = Some(500.millis)
    )
  }

  // Prepare part requests to send to Octoparts
  val requests = Seq(
    // A list of the endpoints you want to call, with parameters to send
    PartRequest(partId = "UserProfile", params = Seq(PartRequestParam("uid", "123"))),
    PartRequest(partId = "LatestNews", params = Seq(PartRequestParam("limit", "10"))),

    // Example of adding a Json string as the "body" parameter for a request
    PartRequest(partId = "TrackView", params = Seq(PartRequestParam("uid", "123"))).withBody(Json.obj("hello" -> "world")),

    // Example of adding a UrlEncodedForm string as the "body" parameter for a request
    PartRequest(
      partId = "TrackClick",
      params = Seq(
        PartRequestParam("uid", "123")
      )
    ).withBody(Map("tabs" -> Seq("1", "2", "3")))

  )

  // Send the requests, get back a Future
  val fResp: Future[AggregateResponse] = octoClient.invoke(123, requests)

  fResp.map { aggregateResponse =>

    // Result will be None if:
    // - Octoparts did not return a response for this endpoint (e.g. because the endpoint was too slow to respond)
    // - the response contained errors
    // - the response could not be deserialized into a UserProfile
    val userProfile: Option[UserProfile] = aggregateResponse.getJsonPart[UserProfile]("UserProfile")

    // Get the list of latest news articles, falling back to an empty list in case of error
    val latestNews: Seq[NewsArticle] = aggregateResponse.getJsonPartOrElse[Seq[NewsArticle]]("LatestNews", default = Nil)

    // Get the list of latest news article, falling back to another json model in case of error
    val userNews: Either[Option[UserProfile], NewsArticle] = aggregateResponse.getJsonPartOrError[NewsArticle, UserProfile]("UserNews")
    userNews.right.toOption == aggregateResponse.getJsonPart[NewsArticle]("UserNews")
    // Do stuff with the user profile and the list of news articles ...

  }

}
