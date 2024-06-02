package uba.fi.peerdy

//#imports
import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import uba.fi.peerdy.actors.OfficeParty
//#imports

object OfficePartyHall {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val office = context.spawn(OfficeParty(), "chatroom")
      Behaviors.empty
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(OfficePartyHall(), "ChatRoomDemo")
  }
}