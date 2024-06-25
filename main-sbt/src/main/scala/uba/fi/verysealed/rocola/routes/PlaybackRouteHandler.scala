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
import uba.fi.verysealed.rocola.SessionManager
import uba.fi.verysealed.rocola.behavior.playlist.PlaybackStatusChanged
import uba.fi.verysealed.rocola.SessionManager._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class PlaybackResponseMessage(message: String, success: Boolean)
object PlaybackResponseMessage {
  implicit val format: RootJsonFormat[PlaybackResponseMessage] = jsonFormat2(PlaybackResponseMessage.apply)
}

object PlaybackRouteHandler {
  def apply(source: Source[PlaybackStatusChanged, _], rocolaManager: ActorRef[RocolaCommand], sessionManager: ActorRef[SessionManager.Command])(implicit system: ActorSystem[_]): Route = {
    implicit val timeout: akka.util.Timeout = 5.seconds
    implicit val ec:ExecutionContext = system.executionContext

    def validateToken(token: String): Future[Boolean] = {
      sessionManager.ask(ref => ValidateToken(token, ref))(timeout, system.scheduler).map(_.valid)
    }

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
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendPlay(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Play started", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to start playing", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("pause") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendPause(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Play paused", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to pause playing", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("stop") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendStop(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Play stopped", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to stop playing", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("skip") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[PlaybackResponse] = rocolaManager.ask(ref => SendSkipSong(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Song skipped", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to skip song", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("volume-up") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendVolumeUp(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Volume increased", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to increase volume", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("volume-down") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendVolumeDown(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Volume decreased", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to decrease volume", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("mute") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendMute(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Volume muted", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to mute volume", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("unmute") {
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SendUnmute(ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Volume unmuted", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to unmute volume", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      } ~
      path("set-volume" / IntNumber) { volume =>
        parameter("token") { token =>
          onSuccess(validateToken(token)) {
            case true =>
              val responseFuture: Future[VolumeControlResponse] = rocolaManager.ask(ref => SetVolume(volume, ref))(timeout, system.scheduler)
              onSuccess(responseFuture) { response =>
                if (response.success) {
                  complete(PlaybackResponseMessage(s"Volume set", success = true))
                } else {
                  complete(PlaybackResponseMessage(s"Failed to set volume", success = false))
                }
              }
            case false => complete(PlaybackResponseMessage("Invalid token", success = false))
          }
        }
      }
  }
}
