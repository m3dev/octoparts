package controllers

import play.api.libs.concurrent.Promise
import play.api.mvc._

object Application extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def slow(delay: Int) = Action.async {
    Promise.timeout(Ok("Sorry for the wait!"), delay)
  }

  def flaky(successRate: Double) = Action {
    if (util.Random.nextDouble <= successRate)
      Ok("No problem!")
    else
      InternalServerError("Whoops!")
  }

}
