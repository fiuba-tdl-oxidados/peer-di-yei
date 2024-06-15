package uba.fi.peerdy.actors.rocola

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal

import java.net.URLEncoder
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object Rocola {

  sealed trait PlaySessionCommand
  final case class StartPlaySession(clientName: String, replyTo: ActorRef[PlaySessionEvent]) extends PlaySessionCommand
  final case class EndPlaySession() extends PlaySessionCommand

  final case class PublishPlaySessionMessage protected(screenName: String, message: String) extends PlaySessionCommand

  sealed trait PlaySessionEvent
  final case class PlaySessionStarted(handle: ActorRef[PostPlayMessage]) extends PlaySessionEvent
  final case class PlaySessionEnded() extends PlaySessionEvent
  final case class PlaySessionDenied(reason:String) extends PlaySessionEvent
  final case class PlayMessagePosted(clientName: String, message: String) extends PlaySessionEvent

  sealed trait RocolaCommand extends PlaySessionCommand
  final case class EnqueueSong(song:String, artist:String) extends RocolaCommand
  final case class Play() extends RocolaCommand
  final case class Pause() extends RocolaCommand
  final case class Stop() extends RocolaCommand
  final case class Skip() extends RocolaCommand
  final case class VolumeUp() extends RocolaCommand
  final case class VolumeDown() extends RocolaCommand
  final case class Mute() extends RocolaCommand
  final case class Unmute() extends RocolaCommand
  final case class SetVolume(volume: Int) extends RocolaCommand
  final case class SetPlaylist() extends RocolaCommand
  final case class PostPlayMessage protected(message: String) extends RocolaCommand
  final case class NotifyDiYei protected(message: PlayMessagePosted) extends RocolaCommand
  private final case class WrappedHttpResponse(response: Try[String]) extends RocolaCommand

  sealed trait SongCommand
  final case class NewSongStarted()
  final case class CurrentSongEnded()

  def apply(): Behavior[PlaySessionCommand] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      implicit val directory:String = "localhost"
      implicit val port:String = "4545"
      val http = Http(system)


      // TODO: refactor this inside the Rocola object, to interact with the Directory Server
      Behaviors.receiveMessage {
        case StartPlaySession(clientName, replyTo) =>
          //TODO: confirm here whether we can use the Rocola or not
          context.log.info(s"Starting play session for $clientName")
          replyTo ! PlaySessionStarted(context.self)
          Behaviors.same
        case EnqueueSong(song:String, artist:String) =>
          val encodedTitle = URLEncoder.encode(song, "UTF-8")
          val encodedArtist = URLEncoder.encode(artist, "UTF-8")

          //TODO: refactor Uri to use a generic method to build the uri
          val uri = Uri(s"http://$directory:$port/enqueue?title=$encodedTitle&artist=$encodedArtist")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)

          context.pipeToSelf(http.singleRequest(request).flatMap { response =>
            Unmarshal(response.entity).to[String]
          }) {
            case Success(value) => WrappedHttpResponse(Success(value))
            case Failure(exception) => WrappedHttpResponse(Failure(exception))
          }
        Behaviors.same
        case Play() =>
          context.log.info("Playing")
          Behaviors.same
        case Pause() =>
          context.log.info("Paused")
          Behaviors.same
        case Stop() =>
          context.log.info("Stopped")
          Behaviors.same
        case Skip() =>
          context.log.info("Skipped")
          Behaviors.same
        case VolumeUp() =>
          context.log.info("Volume Up")
          Behaviors.same
        case VolumeDown() =>
          context.log.info("Volume Down")
          Behaviors.same
        case Mute() =>
          context.log.info("Muted")
          Behaviors.same
        case Unmute() =>
          context.log.info("Unmuted")
          Behaviors.same
        case SetVolume(volume:Int) =>
          context.log.info(s"Volume set to $volume")
          Behaviors.same
        case SetPlaylist() =>
          context.log.info("Playlist set")
          Behaviors.same
        case PostPlayMessage(message) =>
          context.log.info(s"Message posted: $message")
          Behaviors.same
        case NotifyDiYei(message) =>
          context.log.info(s"Message notified: $message")
          Behaviors.same

        case WrappedHttpResponse(response) =>
        response match {
          case Success(res) =>
            //TODO: handle json response to inform the diyei and then inform others
            context.log.info(s"Received response: $res")

          case Failure(exception) =>
            context.log.error(s"Failed to send request: ${exception.getMessage}")
        }
        Behaviors.same

      }
    }
  }
}
