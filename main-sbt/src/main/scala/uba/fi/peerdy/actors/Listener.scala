package uba.fi.peerdy.actors

import uba.fi.peerdy.actors.DiYei.{ProcessCommand, ShowCommands}
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.Member
import uba.fi.peerdy.actors.protocol.PeerProtocol

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
          println("vote <song> <artist> | Pause playlist")
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
            case "vote" =>
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
