package uba.fi.verysealed.rocola.routes

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import uba.fi.verysealed.rocola.RocolaManager._
import uba.fi.verysealed.rocola.behavior.playlist.PlaybackStatusChanged

import scala.concurrent.Future
import scala.concurrent.duration._

// Define the JSON response case class
case class PlaybackResponseMessage(message: String, success: Boolean)

// Define the JSON format for the response
object PlaybackResponseMessage {
  implicit val format: RootJsonFormat[PlaybackResponseMessage] = jsonFormat2(PlaybackResponseMessage.apply)
}

object PlaybackRouteHandler {
  def apply(source: Source[PlaybackStatusChanged, _], rocolaManager: ActorRef[RocolaCommand])(implicit system: ActorSystem[_]): Route = {
    implicit val timeout: akka.util.Timeout = 5.seconds

    path("playback-status") {
      get {
        complete {
          source
            .map { status =>
              ServerSentEvent(status.message)
            }
            .keepAlive(10.second, () => ServerSentEvent.heartbeat)
        }
      }
    } ~
    path("play") {
      get {
        val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendPlay(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Play started", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to start playing", success = false))
          }
        }
      }
    } ~
    path("pause") {
      get {
        val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendPause(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Play paused", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to pause playing", success = false))
          }
        }
      }
    } ~
    path("stop") {
      get {
        val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendStop(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Play stopped", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to stop playing", success = false))
          }
        }
      }
    } ~
    path("skip") {
      get {
        val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendSkipSong(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Song skipped", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to skip song", success = false))
          }
        }
      }
    } ~
    path("volume-up") {
      get {
        val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendVolumeUp(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"volume increased", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to increase volume", success = false))
          }
        }
      }
    } ~
    path("volume-down") {
      get {
        val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendVolumeDown(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Volume decreased", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to decrease volume", success = false))
          }
        }
      }
    } ~
    path("mute") {
      get {
        val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendMute(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Volume muted", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to mute volume", success = false))
          }
        }
      }
    } ~
    path("unmute") {
      get {
        val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendUnmute(ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Volume unmuted", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to unmute volume", success = false))
          }
        }
      }
    } ~
    path("set-volume" / IntNumber) { volume =>
      get {
        val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SetVolume(volume, ref))(timeout, system.scheduler)
        onSuccess(responseFuture) { response =>
          if (response.success) {
            complete(PlaybackResponseMessage(s"Volume set", success = true))
          } else {
            complete(PlaybackResponseMessage(s"Failed to set volume", success = false))
          }
        }
      }
    }
  }
}

