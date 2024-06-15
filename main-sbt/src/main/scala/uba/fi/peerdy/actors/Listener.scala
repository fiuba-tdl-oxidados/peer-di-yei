package uba.fi.peerdy.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Listener {

  sealed trait ListenerCommand
  final case class RequestPlaylist(replyTo: ActorRef[ListenerResponse]) extends ListenerCommand
  final case class VoteSong(songId: String, vote: Boolean, replyTo: ActorRef[ListenerResponse]) extends ListenerCommand
  final case class ProposeSong(song: String, replyTo: ActorRef[ListenerResponse]) extends ListenerCommand
  final case class RequestDJChange(replyTo: ActorRef[ListenerResponse]) extends ListenerCommand

  sealed trait ListenerResponse
  final case class PlaylistResponse(playlist: List[String]) extends ListenerResponse
  final case class VoteResponse(success: Boolean) extends ListenerResponse
  final case class ProposeSongResponse(success: Boolean) extends ListenerResponse
  final case class DJChangeResponse(success: Boolean) extends ListenerResponse

  def apply(dj: ActorRef[DiYei.DiYeiCommand]): Behavior[ListenerCommand] = Behaviors.receive { (context, message) =>
    message match {
      case RequestPlaylist(replyTo) =>
        dj ! DiYei.ProposeSong("listener", "request playlist", "artist")
        replyTo ! PlaylistResponse(List("Song1", "Song2"))
        Behaviors.same
      case VoteSong(songId, vote, replyTo) =>
        context.log.info(s"Voting song $songId with vote: $vote")
        replyTo ! VoteResponse(success = true)
        Behaviors.same
      case ProposeSong(song, replyTo) =>
        dj ! DiYei.ProposeSong("listener", song, "artist")
        replyTo ! ProposeSongResponse(success = true)
        Behaviors.same
      case RequestDJChange(replyTo) =>
        replyTo ! DJChangeResponse(success = false)
        Behaviors.same
    }
  }
}
