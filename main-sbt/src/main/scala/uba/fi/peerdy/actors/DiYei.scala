package uba.fi.peerdy.actors

import akka.actor.typed.{ActorRef, Behavior, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import uba.fi.peerdy.actors.DiYei.rocola
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.rocola.Rocola.{NotifyDiYei, PlayMessagePosted, PlaySessionCommand, PlaySessionDenied, PlaySessionStarted, PublishPlaySessionMessage, Rocola, StartPlaySession}
import uba.fi.peerdy.actors.rocola.behavior.PlayingBehavior

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DiYei {

  sealed trait DiYeiCommand
  final case class ProposeSong(sender: String,song: String) extends DiYeiCommand
  final case class UpVoteSong() extends DiYeiCommand
  final case class DownVoteSong() extends DiYeiCommand

  sealed trait DiYeiEvents
  final case class NewSongAccepted() extends DiYeiEvents
  final case class NewSongRejected() extends DiYeiEvents
  final case class NewVoteAccepted() extends DiYeiEvents
  final case class PlaylistUpdated() extends DiYeiEvents
  final case class CurrentSongUpdated() extends DiYeiEvents

  private var rocola: Option[ActorRef[Rocola.PlaySessionCommand]]= Option.empty

  def apply(): Behavior[DiYeiCommand] = {
    Behaviors.receive { (context, message) =>
      rocola match {
        case None =>
          context.log.info("No Rocola available, creating...")
          rocola = Option {
            context.spawn(Rocola(), "diyeiRocola")
          }
          val handler = context.spawn(RocolaHandler(), "diyeiRocolaHandler")
          rocola.get ! Rocola.StartPlaySession("DiYei", handler)

      }

      message match {
        case ProposeSong(sender:String, song:String) =>
          rocola match {
            case Some(_) =>
              context.log.info(s"$sender is proposing song $song")
              rocola.get ! Rocola.PublishPlaySessionMessage("DiYei", song)
              Behaviors.same
            case None =>
              context.log.info("No Rocola available")
              Behaviors.same
          }
        case UpVoteSong() =>
          rocola match {
            case Some(_) =>
              context.log.info("Upvoting song")
              Behaviors.same
            case None =>
              context.log.info("No Rocola available")
              Behaviors.same
          }
        case DownVoteSong() =>
          rocola match {
            case Some(_) =>
              context.log.info("Downvoting song")
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
