package uba.fi.peerdy.actors

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Terminated}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.rocola.Rocola.{PlaySessionCommand, RocolaCommand}
import uba.fi.peerdy.actors.DiYei.DiYeiCommand
object OfficeParty {
  sealed trait PartyCommand
  final case class StartPlaying(replyTo: ActorRef[PartySessionEvent]) extends PartyCommand
  final case class StartListening(peerName: String, replyTo: ActorRef[PartySessionEvent]) extends PartyCommand
  final case class LeaveParty(peerName: String, replyTo: ActorRef[PartySessionCommand]) extends PartyCommand

  sealed trait PartySessionEvent
  final case class NewDiYeiAccepted(handle: ActorRef[DiYeiCommand]) extends PartySessionEvent
  final case class NewDiYeiDenied(reason: String) extends PartySessionEvent
  final case class NewListenerAccepted(handle: ActorRef[PartySessionCommand]) extends PartySessionEvent
  final case class NewListenerDenied(reason: String) extends PartySessionEvent
  final case class MessagePosted(peerName:String, message: String) extends PartySessionEvent

  sealed trait PartySessionCommand
  final case class PostMessage(message: String) extends PartySessionCommand
  final case class PostCommand(command: DiYeiCommand) extends PartySessionCommand
  private final case class NotifyClient(message: MessagePosted) extends PartySessionCommand

  private final case class PublishSessionMessage(peerName: String, message: String) extends PartyCommand
  private final case class ExecuteSessionCommand(peerName: String, command: DiYeiCommand) extends PartyCommand

  def apply(): Behavior[PartyCommand] = {
    officeParty(Option.empty, List.empty)
  }


    private def officeParty(currentDiYei:Option[ActorRef[DiYeiCommand]],sessions: List[ActorRef[PartySessionCommand]]): Behavior[PartyCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case StartPlaying(client) =>
          currentDiYei match {
            case Some(_) =>
              client ! NewDiYeiDenied("Another session is already in progress. Only one session is allowed at a time.")
              Behaviors.same
            case None =>
              val diYei = context.spawn(DiYei(), "diyei")
              client ! NewDiYeiAccepted(diYei)
              officeParty(Some(diYei), sessions)
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


