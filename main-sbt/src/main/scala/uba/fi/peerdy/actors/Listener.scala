package uba.fi.peerdy.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.Member
import uba.fi.peerdy.actors.protocol.PeerProtocol

object Listener {
  sealed trait ListenerCommand
  final case class ShowCommands() extends ListenerCommand
  final case class ProcessCommand(cmd: String) extends ListenerCommand

  private var rocola: Option[ActorRef[Rocola.RocolaCommand]] = Option.empty
  private var peerProtocol: Option[ActorRef[PeerProtocol.Command]] = Option.empty


  def apply(address: String, port: Int, diyei: Member): Behavior[ListenerCommand] = {
    Behaviors.setup { context =>
      peerProtocol match {
        case Some(_) =>
        case None =>
          peerProtocol = Option { context.spawn(PeerProtocol(), "PeerProtocol") }
          peerProtocol.get ! PeerProtocol.Bind(address, port)
          peerProtocol.get ! PeerProtocol.Connect(diyei.ip, diyei.port)
      }

      rocola match {
        case Some(_) =>
        case None =>
          rocola = Option { context.spawn(Rocola(), "listenersRocola") }
      }
      Behaviors.receiveMessage {
        case ShowCommands() =>
          println("Available commands:")
          println("exit - Exit the system")
          Behaviors.same
        case ProcessCommand(cmd) =>
          cmd match {
            case "commands" =>
              context.self ! ShowCommands()
            case "play" =>
              rocola.get ! Rocola.Play()
            case "do" =>
              peerProtocol.get ! PeerProtocol.SendMessage(s"Ping del listener $port")
            case "do2" =>
              peerProtocol.get ! PeerProtocol.SendMessage(s"NL-$address-$port")
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
