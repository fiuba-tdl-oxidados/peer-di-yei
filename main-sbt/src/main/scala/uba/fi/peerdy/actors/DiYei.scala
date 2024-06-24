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
import scala.collection.mutable
object DiYei {

  sealed trait DiYeiCommand
  final case class ShowCommands() extends DiYeiCommand
  final case class ProcessCommand(cmd: String) extends DiYeiCommand


  private var rocola: Option[ActorRef[Rocola.RocolaCommand]] = Option.empty

  def apply(): Behavior[DiYeiCommand] = {
    Behaviors.setup { context =>
      // var peerProtocol: ActorRef[PeerProtocol.Command] = context.spawn(PeerProtocol(), "peerProtocol")
      // peerProtocol ! PeerProtocol.StartServer("localhost", 8081)

      rocola match {
        case Some(_) =>
          context.log.info("Rocola found")
        case None =>
          context.log.info("No Rocola available, creating...")
          rocola = Option { context.spawn(Rocola(), "diyeiRocola") }
      }
      Behaviors.receiveMessage {
        case ShowCommands() =>
          println("Available commands:")
          println("play - Play playlist")
          println("pause - Pause playlist")
          println("commands - List available commands")
          println("exit - Exit the system")
          Behaviors.same
        case ProcessCommand(cmd) =>
          cmd match {
            case "commands" =>
              context.self ! ShowCommands()
            case "play" =>
              rocola.get ! Rocola.Play()
            case "pause" =>
              rocola.get ! Rocola.Pause()
            case "propose" =>
              println("Enter song name: ")
              val song = scala.io.StdIn.readLine()
              println("Enter artist name: ")
              val artist = scala.io.StdIn.readLine()
              rocola.get ! Rocola.EnqueueSong(song, artist)
            case _ =>
              context.log.info("Invalid command")
          }
          Behaviors.same
      }
    }
  }
}
