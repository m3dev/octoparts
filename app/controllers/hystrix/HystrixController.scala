package controllers.hystrix

import java.nio.charset.StandardCharsets

import play.api.mvc._

class HystrixController(
    streamer: Streamer,
    controllerComponents: ControllerComponents
) extends AbstractController(controllerComponents) {

  private val ChunkedHeaders = Seq(
    "Content-Type" -> s"text/event-stream; charset=${StandardCharsets.UTF_8}",
    "Cache-Control" -> Seq("no-cache", "no-store", "max-age=0", "must-revalidate").mkString(", "),
    "Pragma" -> "no-cache"
  )

  def stream(delayMs: Option[Int]) = Action {
    Ok.chunked(streamer.source).withHeaders(ChunkedHeaders: _*)
  }

}