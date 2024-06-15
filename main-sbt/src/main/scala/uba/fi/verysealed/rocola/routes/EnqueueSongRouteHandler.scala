package uba.fi.verysealed.rocola.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import uba.fi.verysealed.rocola.RocolaManager

import scala.concurrent.Future
import scala.concurrent.duration._

// Define the JSON response case class
case class EnqueueResponse(message: String, success: Boolean)

// Define the JSON format for the response
object EnqueueResponse {
  implicit val format: RootJsonFormat[EnqueueResponse] = jsonFormat2(EnqueueResponse.apply)
}
class EnqueueSongRouteHandler(rocolaManager: ActorRef[RocolaManager.RocolaCommand], system: ActorSystem[RocolaManager.RocolaCommand])(implicit timeout: Timeout) {

  val route: Route =
    path("enqueue") {
      parameters("title", "artist") { (title, artist) =>
        val responseFuture: Future[RocolaManager.EnqueueSongResponse] = rocolaManager
          .ask(ref => RocolaManager.EnqueueSong(title, artist, ref))(timeout, system.scheduler)

        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(EnqueueResponse(s"Enqueued song: $title by $artist", success = true))
          } else {
            complete(EnqueueResponse(s"Failed to enqueue song: $title by $artist", success = false))
          }
        }
      }
    }
}


