package uba.fi.peerdy

//#imports
import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.OfficeParty
import uba.fi.peerdy.actors.rocola.Rocola
//#imports

object OfficePartyHall {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val officeParty = context.spawn(OfficeParty(), "officeParty")
      //TODO: understand how to mix behaviors in the same actor
      officeParty ! NotUsed
      Behaviors.empty
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(OfficePartyHall(), "ChatRoomDemo")
  }
}