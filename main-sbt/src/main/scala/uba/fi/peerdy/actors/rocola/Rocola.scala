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
  sealed trait RocolaCommand
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

  def apply(): Behavior[RocolaCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case EnqueueSong(song, artist) =>
          context.log.info(s"Adding $song to the queue, by $artist")
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
      }
    }
  }
}
