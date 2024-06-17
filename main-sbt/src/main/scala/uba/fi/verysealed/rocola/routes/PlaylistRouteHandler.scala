package uba.fi.verysealed.rocola.routes

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import uba.fi.verysealed.rocola.RocolaManager
import uba.fi.verysealed.rocola.behavior.SongMetadata

import scala.concurrent.Future

// Define the JSON response case class
case class PlaylistResponseMessage(message: String, success: Boolean, playlist: List[SongMetadata])

// Define the JSON format for the response
object PlaylistResponseMessage {
  implicit val format: RootJsonFormat[PlaylistResponseMessage] = jsonFormat3(PlaylistResponseMessage.apply)
}

// Define the JSON request case class for POST dequeue
case class DequeueRequest(title: String, artist: String)

// Define the JSON format for the request
object DequeueRequest {
  implicit val format: RootJsonFormat[DequeueRequest] = jsonFormat2(DequeueRequest.apply)
}

class PlaylistRouteHandler(rocolaManager: ActorRef[RocolaManager.RocolaCommand], system: ActorSystem[RocolaManager.RocolaCommand])(implicit timeout: Timeout) {

  val route: Route =
    path("enqueue") {
      parameters("title", "artist", "votes") { (title, artist, votes) =>

        val responseFuture: Future[RocolaManager.PlaylistResponse] = rocolaManager
          .ask(ref => RocolaManager.EnqueueSong(title, artist, votes.toInt, ref))(timeout, system.scheduler)

        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaylistResponseMessage(s"Enqueued song: $title by $artist", success = true, response.playlist))
          } else {
            complete(PlaylistResponseMessage(s"Failed to enqueue song: $title by $artist", success = false, response.playlist))
          }
        }
      }
    } ~
      path("dequeue") {
        get {
          parameters("ordinal".as[Int]) { ordinal =>
            val responseFuture: Future[RocolaManager.PlaylistResponse] = rocolaManager
              .ask(ref => RocolaManager.DequeueSongByOrdinal(ordinal, ref))(timeout, system.scheduler)

            onSuccess(responseFuture) { response =>
              if (response.success) {
                complete(PlaylistResponseMessage(s"Dequeued song with ordinal: $ordinal", success = true, response.playlist))
              } else {
                complete(PlaylistResponseMessage(s"Failed to dequeue song with ordinal: $ordinal", success = false, response.playlist))
              }
            }
          }
        } ~
          post {
            entity(as[DequeueRequest]) { dequeueRequest =>
              val responseFuture: Future[RocolaManager.PlaylistResponse] = rocolaManager
                .ask(ref => RocolaManager.DequeueSong(dequeueRequest.title, dequeueRequest.artist, ref))(timeout, system.scheduler)

              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaylistResponseMessage(s"Dequeued song: ${dequeueRequest.title} by ${dequeueRequest.artist}", success = true, response.playlist))
                } else {
                  complete(PlaylistResponseMessage(s"Failed to dequeue song: ${dequeueRequest.title} by ${dequeueRequest.artist}", success = false, response.playlist))
                }
              }
            }
          }
      } ~
      path("list") {
        val responseFuture: Future[RocolaManager.PlaylistResponse] = rocolaManager
          .ask(ref => RocolaManager.AskPlaylist(ref))(timeout, system.scheduler)

        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaylistResponseMessage(s"Playlist successfully obtained!", success = true, response.playlist))
          } else {
            complete(PlaylistResponseMessage(s"Failed to get playlist", success = false, List.empty))
          }
        }
      }
}


