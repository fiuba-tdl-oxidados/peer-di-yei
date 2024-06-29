package uba.fi.peerdy.actors.rocola

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import spray.json._


import java.net.URLEncoder
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

case class SongMetadata(title: String, artist: String, genre: String, duration: Int, votes:Int = 0)
// Define the JSON format for SongMetadata

case class PlaylistResponseMessage(message: String, success: Boolean, playlist: List[SongMetadata])
case class PlaybackResponseMessage(message: String, success: Boolean)
trait JsonSupport extends DefaultJsonProtocol {
  implicit val songMetadataFormat: RootJsonFormat[SongMetadata] = jsonFormat5(SongMetadata.apply)
  implicit val playbackResponseMessageFormat: RootJsonFormat[PlaybackResponseMessage] = jsonFormat2(PlaybackResponseMessage.apply)
  implicit val playlistResponseFormat: RootJsonFormat[PlaylistResponseMessage] = jsonFormat3(PlaylistResponseMessage.apply)
}
object Rocola extends JsonSupport {
  sealed trait RocolaCommand
  final case class EnqueueSong(song:String, artist:String) extends RocolaCommand
  final case class Play() extends RocolaCommand
  final case class Pause() extends RocolaCommand
  final case class Stop() extends RocolaCommand
  final case class Skip() extends RocolaCommand
  final case class List() extends RocolaCommand
  final case class VolumeUp() extends RocolaCommand
  final case class VolumeDown() extends RocolaCommand
  final case class Mute() extends RocolaCommand
  final case class Unmute() extends RocolaCommand
  final case class SetVolume(volume: Int) extends RocolaCommand
  final case class SetPlaylist() extends RocolaCommand

  sealed trait SongCommand
  final case class NewSongStarted()
  final case class CurrentSongEnded()

  def apply(): Behavior[RocolaCommand] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      val http = Http(system)
      val directory = "localhost"
      val port = "4545"

      Behaviors.receiveMessage {
        case EnqueueSong(song:String, artist:String) =>
          context.log.info(s"Enqui.. song: $song")
          val uri = Uri(s"http://$directory:$port/enqueue?title=$song&artist=$artist&votes=1")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaylistResponseMessage]
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case Play() =>
          context.log.info("Playing")
          val uri = Uri(s"http://$directory:$port/play")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Playing")
                    case false => context.log.info(s"Failed to play ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case Pause() =>
          context.log.info("Paused")
          val uri = Uri(s"http://$directory:$port/pause")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Paused")
                    case false => context.log.info(s"Failed to pause ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case Stop() =>
          context.log.info("Stopped")
          val uri = Uri(s"http://$directory:$port/stop")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Stopped")
                    case false => context.log.info(s"Failed to stop ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case Skip() =>
          context.log.info("Skipped")
          val uri = Uri(s"http://$directory:$port/skip")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Skipped")
                    case false => context.log.info(s"Failed to skip ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
          Behaviors.same
        case VolumeUp() =>
          context.log.info("Volume Up")
          val uri = Uri(s"http://$directory:$port/volume-up")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Volume Up")
                    case false => context.log.info(s"Failed to increase volume ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case VolumeDown() =>
          context.log.info("Volume Down")
          val uri = Uri(s"http://$directory:$port/volume-down")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Volume Down")
                    case false => context.log.info(s"Failed to decrease volume ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case List() =>
          val uri = Uri(s"http://$directory:$port/list")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaylistResponseMessage]
                  response.success match {
                    case true => println(response.playlist)
                    case false => context.log.info(s"Failed to get list ${response.message}")
                  }
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case Mute() =>
          context.log.info("Muted")
          val uri = Uri(s"http://$directory:$port/mute")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Muted")
                    case false => context.log.info(s"Failed to mute volume ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case Unmute() =>
          context.log.info("Unmuted")
          val uri = Uri(s"http://$directory:$port/unmute")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => context.log.info("Unmuted")
                    case false => context.log.info(s"Failed to unmute volume ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case SetVolume(volume:Int) =>
          context.log.info(s"Volume set to $volume")
          val uri = Uri(s"http://$directory:$port/set-volume/$volume")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaybackResponseMessage]
                  response.success match {
                    case true => println(s"Volume set to $volume")
                    case false => println(s"Failed to set volume to $volume ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
        case SetPlaylist() =>
          context.log.info("Playlist set")
          val uri = Uri(s"http://$directory:$port/set-playlist")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          responseFuture.onComplete {
            case Success(response) =>
              Unmarshal(response.entity).to[String].onComplete {
                case Success(value) =>
                  val response = value.parseJson.convertTo[PlaylistResponseMessage]
                  response.success match {
                    case true => context.log.info("Playlist set")
                    case false => context.log.info(s"Failed to set playlist ${response.message}")
                  }
                  println(s"Response: $response")
                case Failure(error) =>
                  println(s"Failed to unmarshal response: $error")
              }
            case Failure(exception) =>
              println(s"Request failed: $exception")
          }
          Behaviors.same
      }
    }
  }
}
