package uba.fi.peerdy.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
import scala.collection.mutable
import java.net.URLEncoder

import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.protocol.PeerProtocol

object DiYei {
  sealed trait DiYeiCommand
  final case class ShowCommands() extends DiYeiCommand
  final case class ProcessCommand(cmd: String) extends DiYeiCommand

  def apply(address: String, port: Int): Behavior[DiYeiCommand] = {
    Behaviors.setup { context =>
      var rocola: ActorRef[Rocola.RocolaCommand] = context.spawn(Rocola(), "diyeiRocola")

      var peerProtocol: ActorRef[PeerProtocol.Command] = context.spawn(PeerProtocol(), "peerProtocol")
      peerProtocol ! PeerProtocol.Bind(address, port)

      Behaviors.receiveMessage {
        case ShowCommands() =>
          println("Available commands:")
          println("commands | List available commands")
          println("play | Play playlist")
          println("pause | Pause playlist")
          println("skip | Skip to next song")
          println("list | List the playlist")
          println("volumeup | Adds 1 to the current volume")
          println("volumedown | Takes away 1 to the current volume")
          println("volume N | Set the volume to N")
          println("mute | Mutes")
          println("unmute | Unmutes")
          println("propose <song> <artist> | Pause playlist")
          println("exit | Exit the system")
          Behaviors.same
        case ProcessCommand(msg) =>
          val args = msg.split(" ")
          val cmd = args(0)
          cmd match {
            case "commands" =>
              context.self ! ShowCommands()
            case "play" =>
              rocola ! Rocola.Play()
            case "pause" =>
              rocola ! Rocola.Pause()
            case "skip" =>
              rocola ! Rocola.Skip()
            case "list" =>
              rocola ! Rocola.List()
            case "volumeup" =>
              rocola ! Rocola.VolumeUp()
            case "volumedown" =>
              rocola ! Rocola.VolumeDown()
            case "volume" =>
              rocola ! Rocola.SetVolume(args(1).toInt)
            case "mute" =>
              rocola ! Rocola.Mute()
            case "unmute" =>
              rocola ! Rocola.Unmute()
            case "propose" =>
              rocola ! Rocola.EnqueueSong(args(1), args(2))
            case "do" =>
              peerProtocol ! PeerProtocol.SendMessage("Ping del diYei")
            case _ =>
              context.log.info("Invalid command")
          }
          Behaviors.same
      }
    }
  }
}
