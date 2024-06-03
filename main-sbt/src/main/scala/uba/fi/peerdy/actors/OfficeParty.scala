package uba.fi.peerdy.actors

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Terminated}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import uba.fi.peerdy.actors.rocola.Rocola
import uba.fi.peerdy.actors.rocola.Rocola.{PlaySessionCommand, RocolaCommand}

object OfficeParty {
  sealed trait PartyCommand
  final case class StartListening(peerName: String, replyTo: ActorRef[PartySessionEvent]) extends PartyCommand
  final case class StopListening(peerName: String, replyTo: ActorRef[PartySessionCommand]) extends PartyCommand

  sealed trait PartySessionEvent
  final case class JoinRequestAccepted(handle: ActorRef[PostMessage]) extends PartySessionEvent
  final case class JoinRequestDenied(reason: String) extends PartySessionEvent
  final case class MessagePosted(screenName: String, message: String) extends PartySessionEvent

  sealed trait PartySessionCommand
  final case class PostMessage(message: String) extends PartySessionCommand
  private final case class NotifyClient(message: MessagePosted) extends PartySessionCommand


  sealed trait DiYeiCommand
  final case class ProposeSong() extends DiYeiCommand
  final case class UpVoteSong() extends DiYeiCommand
  final case class DownVoteSong() extends DiYeiCommand

  sealed trait DiYeiEvents
  final case class NewSongAccepted() extends DiYeiEvents
  final case class NewSongRejected() extends DiYeiEvents
  final case class NewVoteAccepted() extends DiYeiEvents
  final case class PlaylistUpdated() extends DiYeiEvents
  final case class CurrentSongUpdated() extends DiYeiEvents

  private final case class PublishSessionMessage(screenName: String, message: String) extends PartyCommand

  def apply(): Behavior[NotUsed] =
    Behaviors.receive { (context, message) =>
      val handlerRef = context.spawn(RocolaHandler(), "handler")
      val rocola = context.spawn(Rocola(), "rocola")
      context.watch(handlerRef)

      rocola ! Rocola.StartPlaySession("rocola", handlerRef)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }

  private object RocolaHandler {
    import Rocola._

    def apply(): Behavior[PlaySessionEvent] =
      Behaviors.setup { context =>
        Behaviors.receiveMessage {
          case PlaySessionDenied(reason) =>
            context.log.info("cannot start chat room session: {}", reason)
            Behaviors.stopped
          case PlaySessionStarted(handle) =>
            handle ! PostPlayMessage("Hello World!")
            Behaviors.same
          case PlayMessagePosted(clientName, message) =>
            context.log.info("message has been posted by '{}': {}", clientName, message)
            Behaviors.stopped
        }
      }

  private def officeParty(sessions: List[ActorRef[PartySessionCommand]]): Behavior[PartyCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case StartListening(peerName, client) =>
          // create a child actor for further interaction with the client
          val ses = context.spawn(
            session(context.self, peerName, client),
            name = URLEncoder.encode(peerName, StandardCharsets.UTF_8.name))
          client ! JoinRequestAccepted(ses)
          officeParty(ses :: sessions)
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }

  private def session(
                       party: ActorRef[PublishSessionMessage],
                       peerName: String,
                       client: ActorRef[PartySessionEvent]): Behavior[PartySessionCommand] =
    Behaviors.receiveMessage {
      case PostMessage(message) =>
        // from client, publish to others via the room
        party ! PublishSessionMessage(peerName, message)
        Behaviors.same
      case NotifyClient(message) =>
        // published from the room
        client ! message
        Behaviors.same
    }
  }
}
