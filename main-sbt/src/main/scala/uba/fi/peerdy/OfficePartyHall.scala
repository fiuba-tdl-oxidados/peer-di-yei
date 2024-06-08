package uba.fi.peerdy

//#imports
import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.DiYei.DiYeiCommand
import uba.fi.peerdy.actors.OfficeParty.{NewDiYeiAccepted, NewDiYeiDenied, NewListenerAccepted, NewListenerDenied, PartySessionEvent, StartPlaying}
import uba.fi.peerdy.actors.{DiYei, OfficeParty}

//#imports

object OfficePartyHall {
  private var diyeiProxy:  Option[ActorRef[DiYeiCommand]] = Option.empty
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val officeParty = context.spawn(OfficeParty(), "officeParty")
      val handlerRef = context.spawn(SessionHandler(), "handler")
      //TODO: understand how to mix behaviors in the same actor
      officeParty ! StartPlaying(handlerRef)
      Behaviors.empty

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }


  def main(args: Array[String]): Unit = {
    ActorSystem(OfficePartyHall(), "ChatRoomDemo")
  }

  private def SessionHandler(): Behavior[PartySessionEvent] = {
    Behaviors.receive { (context, message) =>
      message match {
        case NewDiYeiAccepted(handle) =>
          context.log.info("DiYei accepted")
          diyeiProxy = Option(handle)
          while (true) {
            val input = scala.io.StdIn.readLine()
            if (input == "exit") {
              context.log.info("Exiting")
              return Behaviors.stopped
            }
            translateDiYeiCommand(input)
          }
          Behaviors.same
        case NewDiYeiDenied(reason) =>
          context.log.info(s"DiYei denied: $reason")
          Behaviors.stopped
        case NewListenerAccepted(handle) =>
          context.log.info("Listener accepted")
          Behaviors.same
        case NewListenerDenied(reason) =>
          context.log.info(s"Listener denied: $reason")
          Behaviors.stopped
        case _ =>
          Behaviors.same
      }
    }
  }

  private def translateDiYeiCommand(str: String): Unit = {
    str match {
      case "propose" =>
        val song = scala.io.StdIn.readLine()
        println(s"Proposing song: $song")
        diyeiProxy.get ! DiYei.ProposeSong("DiYei",song)
      case "upvote" =>
        println("Upvoting song")
      case "downvote" =>
        println("Downvoting song")
      case _ =>
        println("Invalid command")
    }
  }

}