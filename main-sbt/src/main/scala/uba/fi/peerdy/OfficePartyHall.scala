package uba.fi.peerdy

//#imports
import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.OfficeParty.{PartySessionCommand, PartySessionEvent, StartPlaying}
import uba.fi.peerdy.actors.OfficeParty

//#imports

object OfficePartyHall {

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
        case _ =>
          Behaviors.same
      }
    }
  }
}