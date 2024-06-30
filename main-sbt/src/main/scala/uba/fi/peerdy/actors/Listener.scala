package uba.fi.peerdy.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.Member
import uba.fi.peerdy.actors.protocol.PeerProtocol
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}

object Listener {
  sealed trait ListenerCommand
  final case class ShowCommands() extends ListenerCommand
  final case class ProcessCommand(cmd: String) extends ListenerCommand


  def apply(address: String, port: Int, diyei: Member): Behavior[ListenerCommand] = {
    Behaviors.setup { context =>
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      
      var rocola: ActorRef[Rocola.RocolaCommand] = context.spawn(Rocola(), "listenersRocola")

      var peerProtocol: ActorRef[PeerProtocol.Command] = context.spawn(PeerProtocol(), "peerProtocol")
      peerProtocol ! PeerProtocol.Bind(address, port)
      
      val promise = Promise[Unit]()
      val future: Future[Unit] = promise.future
      peerProtocol ! PeerProtocol.Connect(diyei.ip, diyei.port, promise)
      future.onComplete {
        case Success(_) => peerProtocol ! PeerProtocol.SendMessage(s"NL-$address-$port")
        case Failure(exception) => println("ACA FALLO")
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
              rocola ! Rocola.Play()
            case "ping" =>
              peerProtocol ! PeerProtocol.SendMessage(s"Ping del listener $port")
            case "present" =>
              peerProtocol ! PeerProtocol.SendMessage(s"NL-$address-$port")
            case "pause" =>
              rocola ! Rocola.Pause()
            case "propose" =>
              println("Enter song name: ")
              val song = scala.io.StdIn.readLine()
              println("Enter artist name: ")
              val artist = scala.io.StdIn.readLine()
              rocola ! Rocola.EnqueueSong(song, artist)
            case _ =>
              context.log.info("Invalid command")
          }
          Behaviors.same
      }
    }
  }
}
