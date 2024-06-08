package uba.fi.peerdy.actors.rocola

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import uba.fi.peerdy.actors.rocola.behavior.AuthenticationBehavior

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

  sealed trait SongCommand
  final case class NewSongStarted()
  final case class CurrentSongEnded()

  def apply(): Behavior[PlaySessionCommand] =
    Behaviors.setup(context => new AuthenticationBehavior(context))

  class Rocola {
    def play(): Unit = {
      println("Playing")
    }
    def pause(): Unit = {
      println("Paused")
    }
    def stop(): Unit = {
      println("Stopped")
    }
    def skip(): Unit = {
      println("Skipped")
    }
    def volumeUp(): Unit = {
      println("Volume Up")
    }
    def volumeDown(): Unit = {
      println("Volume Down")
    }
    def mute(): Unit = {
      println("Muted")
    }
    def unmute(): Unit = {
      println("Unmuted")
    }
    def setVolume(volume: Int): Unit = {
      println(s"Volume set to $volume")
    }
    def setPlaylist(): Unit = {
      println("Playlist set")
    }
    def postPlayMessage(message: String): Unit = {
      println(s"Message posted: $message")
    }
    def notifyDiYei(message: PlayMessagePosted): Unit = {
      println(s"Message notified: $message")
    }
  }
}
