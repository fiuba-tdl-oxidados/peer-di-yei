package uba.fi.verysealed.rocola

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import uba.fi.peerdy.actors.OfficeParty.PartySessionEvent
import uba.fi.verysealed.rocola.behavior.{PlayingBehavior, SongMetadata}

object RocolaManager {

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
  final case class EnqueueSong(title:String, artist:String, votes:Integer, replyTo: ActorRef[PlaylistResponse]) extends RocolaCommand
  final case class DequeueSong(title:String, artist:String, replyTo: ActorRef[PlaylistResponse]) extends RocolaCommand
  final case class DequeueSongByOrdinal(ordinal: Int, replyTo: ActorRef[PlaylistResponse])extends RocolaCommand
  final case class VoteSong(votePositive: Boolean, title:String, artist:String, replyTo: ActorRef[PlaylistResponse])extends RocolaCommand
  final case class VoteSongByOrdinal(votePositive: Boolean, ordinal: Int, replyTo: ActorRef[PlaylistResponse])extends RocolaCommand


  final case class SetPlaylist(playlist: List[SongMetadata],replyTo:ActorRef[PlaylistResponse]) extends RocolaCommand
  final case class AskPlaylist(replyTo:ActorRef[PlaylistResponse]) extends RocolaCommand
  case class PlaylistResponse(success: Boolean, message:String, playlist: List[SongMetadata])

  final case class SendPlay(replyTo: ActorRef[PlaybackResponse]) extends RocolaCommand
  final case class SendPause(replyTo: ActorRef[PlaybackResponse]) extends RocolaCommand
  final case class SendStop(replyTo: ActorRef[PlaybackResponse]) extends RocolaCommand
  final case class SendSkipSong(replyTo: ActorRef[PlaybackResponse]) extends RocolaCommand
  case class PlaybackResponse(success: Boolean, message: String)

  final case class SendVolumeUp(replyTo: ActorRef[VolumeControlResponse]) extends RocolaCommand
  final case class SendVolumeDown(replyTo: ActorRef[VolumeControlResponse]) extends RocolaCommand
  final case class SendMute(replyTo: ActorRef[VolumeControlResponse]) extends RocolaCommand
  final case class SendUnmute(replyTo: ActorRef[VolumeControlResponse]) extends RocolaCommand
  final case class SetVolume(volume: Int,replyTo: ActorRef[VolumeControlResponse]) extends RocolaCommand
  case class VolumeControlResponse(success: Boolean, message: String)


  final case class PostPlayMessage protected(message: String) extends RocolaCommand
  final case class NotifyDiYei protected(message: PlayMessagePosted) extends RocolaCommand



  def apply(): Behavior[RocolaCommand] =
    Behaviors.setup(context => new PlayingBehavior(context))



}
