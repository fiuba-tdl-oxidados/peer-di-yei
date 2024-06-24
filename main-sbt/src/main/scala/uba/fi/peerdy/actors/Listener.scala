package uba.fi.peerdy.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.Member

object Listener {
  sealed trait ListenerCommand
  final case class ShowCommands() extends ListenerCommand
  final case class ProcessCommand(cmd: String) extends ListenerCommand

  private var rocola: Option[ActorRef[Rocola.RocolaCommand]] = Option.empty


  def apply(diyei: Member): Behavior[ListenerCommand] = {
    Behaviors.setup { context =>
      // var peerProtocol: ActorRef[PeerProtocol.Command] = context.spawn(PeerProtocol(), "peerProtocol")
      // peerProtocol ! PeerProtocol.AddConnection(diyei.ip, diyei.port)
      // peerProtocol ! PeerProtocol.SendMessageToAll("NewListener")

      rocola match {
        case Some(_) =>
          context.log.info("Rocola found")
        case None =>
          context.log.info("No Rocola available, creating...")
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
