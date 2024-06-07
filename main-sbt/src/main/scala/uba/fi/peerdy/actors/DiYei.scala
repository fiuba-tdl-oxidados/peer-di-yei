package uba.fi.peerdy.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.rocola.Rocola.Rocola

object DiYei {

  sealed trait DiYeiCommand
  final case class ProposeSong() extends DiYeiCommand
  final case class UpVoteSong() extends DiYeiCommand
  final case class DownVoteSong() extends DiYeiCommand

  sealed trait DiYeiEvents
  final case class NewSongAccepted() extends DiYeiEvents
  final case class NewSongRejected() extends DiYeiEvents
  final case class NewVoteAccepted() extends DiYeiEvents
  final case class PlaylistUpdated() extends DiYeiEvents
  final case class CurrentSongUpdated() extends DiYeiEvents

  def apply(): Behavior[DiYeiCommand] = {
    rockDj(Option.empty)
  }

  private def rockDj(rocola:Option[Rocola]) : Behavior[DiYeiCommand] = {
    Behaviors.receive { (context, message) =>
      message match {
        case ProposeSong() =>
          rocola match {
            case Some(_) =>
              context.log.info("Proposing song")
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
}
