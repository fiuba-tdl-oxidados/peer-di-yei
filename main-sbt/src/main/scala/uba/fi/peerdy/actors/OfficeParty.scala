package uba.fi.peerdy.actors


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import uba.fi.peerdy.actors.DiYei.DiYeiCommand
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/*
 * OfficeParty handles a specific party session once started.
 * It is responsible for creating the DiYei actor and handling the communication
 * between the DiYei and the listeners.
 */
object OfficeParty {

  sealed trait PartyCommand

  final case class StartPlaying(replyTo: ActorRef[PartySessionEvent]) extends PartyCommand

  final case class StartListening(peerName: String, replyTo: ActorRef[PartySessionEvent]) extends PartyCommand

  final case class LeaveParty(peerName: String, replyTo: ActorRef[PartySessionCommand]) extends PartyCommand

  sealed trait PartySessionEvent

  final case class NewDiYeiAccepted(handle: ActorRef[Listener.ListenerCommand]) extends PartySessionEvent

  final case class NewDiYeiDenied(reason: String) extends PartySessionEvent

  final case class NewListenerAccepted(handle: ActorRef[PartySessionCommand]) extends PartySessionEvent

  final case class NewListenerDenied(reason: String) extends PartySessionEvent

  final case class MessagePosted(peerName: String, message: String) extends PartySessionEvent

  sealed trait PartySessionCommand

  final case class PostMessage(message: String) extends PartySessionCommand

  final case class PostCommand(command: DiYeiCommand) extends PartySessionCommand

  private final case class NotifyClient(message: MessagePosted) extends PartySessionCommand

  private final case class PublishSessionMessage(peerName: String, message: String) extends PartyCommand

  private final case class ExecuteSessionCommand(peerName: String, command: DiYeiCommand) extends PartyCommand

  // using FP to define behavior as a function in this object
  def apply(): Behavior[PartyCommand] = {
    officeParty(Option.empty, List.empty)
  }

  /*
   * The officeParty behavior is the main behavior of the OfficeParty actor.
   * It handles the messages received by the OfficeParty actor.
   * The OfficeParty actor can start a new DiYei session or a new listener session.
   * It can also publish messages to all the listeners in the session.
   * The OfficeParty actor can also execute commands on the DiYei.
   */
  private def officeParty(
                           currentDiYei: Option[ActorRef[DiYeiCommand]],
                           sessions: List[ActorRef[PartySessionCommand]]): Behavior[PartyCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case StartPlaying(client) =>
          // check whether a currentDiYei is already in place
          // TODO: here we should check against directory whether this client could act as Diyei if nobody has already started a session
          currentDiYei match {
            case Some(_) =>
              client ! NewDiYeiDenied("Another session is already in progress. Only one session is allowed at a time.")
              Behaviors.same
            case None =>
              val diYei = context.spawn(Listener(), "listener")
              client ! NewDiYeiAccepted(diYei)
              Behaviors.same
          }
        case StartListening(peerName, client) =>
          currentDiYei match {
            case None =>
              client ! NewListenerDenied("No session is currently active. Please start a session first.")
              Behaviors.same
            case Some(_) =>
              val ses = context.spawn(
                session(context.self, peerName, client),
                name = URLEncoder.encode(peerName, StandardCharsets.UTF_8.name))
              client ! NewListenerAccepted(ses)
              officeParty(currentDiYei, ses :: sessions)
          }

        case PublishSessionMessage(peerName, message) =>
          val notification = NotifyClient(MessagePosted(peerName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same

        case ExecuteSessionCommand(peerName, command) =>
          currentDiYei match {
            case None =>
              context.log.warn("No session is currently active. Please start a session first.")
            case Some(diYei) =>
              diYei ! command
          }
          val notification = NotifyClient(MessagePosted(peerName, command.toString))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }

  private def session(
                       party: ActorRef[PartyCommand],
                       peerName: String,
                       client: ActorRef[PartySessionEvent]): Behavior[PartySessionCommand] =
    Behaviors.receiveMessage {
      case PostMessage(message) =>
        // from client, publish to others via the room
        party ! PublishSessionMessage(peerName, message)
        Behaviors.same
      case PostCommand(command) =>
        // from client, publish to others via the room
        party ! ExecuteSessionCommand(peerName, command)
        Behaviors.same
      case NotifyClient(message) =>
        // published from the room
        client ! message
        Behaviors.same
    }


}


