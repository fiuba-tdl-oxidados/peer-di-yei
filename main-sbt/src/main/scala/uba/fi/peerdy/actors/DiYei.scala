package uba.fi.peerdy.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.Timeout
import uba.fi.peerdy.actors.rocola.Rocola

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
import java.net.URLEncoder
object DiYei {

  sealed trait DiYeiCommand
  final case class ProposeSong(sender: String,song: String, artist:String) extends DiYeiCommand
  final case class UpVoteSong(song: String) extends DiYeiCommand
  final case class DownVoteSong(song: String) extends DiYeiCommand

  sealed trait DiYeiEvents
  final case class NewSongAccepted() extends DiYeiEvents
  final case class NewSongRejected() extends DiYeiEvents
  final case class NewVoteAccepted() extends DiYeiEvents
  final case class PlaylistUpdated() extends DiYeiEvents
  final case class CurrentSongUpdated() extends DiYeiEvents

  private var rocola: Option[ActorRef[Rocola.PlaySessionCommand]]= Option.empty

  def apply(): Behavior[DiYeiCommand] = {

    Behaviors.setup { context =>

      rocola match {
        case None =>
          context.log.info("No Rocola available, creating...")
          rocola = Option {
            context.spawn(Rocola(), "diyeiRocola")
          }
          val handler = context.spawn(RocolaHandler(), "diyeiRocolaHandler")
          rocola.get ! Rocola.StartPlaySession("DiYei", handler)

      }
      // TODO: refactor this inside the Rocola object, to interact with the Directory Server
      Behaviors.receiveMessage {

        case ProposeSong(sender: String, song: String, artist:String) =>
          //TODO: refactor to avoid duplicates when checking for rocola availability
          rocola match {
            case Some(_) =>
              rocola.get ! Rocola.EnqueueSong(song,artist)
              Behaviors.same
            case None =>
              //TODO: change behavior if no rocola is available?
              context.log.info("No Rocola available")
              Behaviors.same
          }
        case UpVoteSong(song: String) =>
          rocola match {
            case Some(_) =>
              context.log.info("Upvoting song")
              //TODO: implement UpVoteSong
              Behaviors.same
            case None =>
              context.log.info("No Rocola available")
              Behaviors.same
          }
        case DownVoteSong(song: String) =>
          rocola match {
            case Some(_) =>
              context.log.info("Downvoting song")
              //TODO: implement DownVoteSong
              Behaviors.same
            case None =>
              context.log.info("No Rocola available")
              Behaviors.same
          }
      }
  }
    }

  private object RocolaHandler
  {
    def apply(): Behavior[Rocola.PlaySessionEvent] = {
      Behaviors.receive { (context, message) =>
        message match {
          case Rocola.PlaySessionStarted(handle) =>
            context.log.info("Play session started")
            Behaviors.same
          case Rocola.PlaySessionEnded() =>
            context.log.info("Play session ended")
            Behaviors.same
          case Rocola.PlaySessionDenied(reason) =>
            context.log.info(s"Play session denied: $reason")
            Behaviors.same
          case Rocola.PlayMessagePosted(clientName, message) =>
            context.log.info(s"Message posted by $clientName: $message")
            Behaviors.same
        }
      }
    }
  }


}
