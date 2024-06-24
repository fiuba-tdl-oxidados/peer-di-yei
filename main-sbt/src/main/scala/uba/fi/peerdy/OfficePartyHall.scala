package uba.fi.peerdy

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import uba.fi.peerdy.actors.DiYei._
import uba.fi.peerdy.actors.OfficeParty._
import uba.fi.peerdy.actors.Listener._
import uba.fi.peerdy.actors.{DiYei, OfficeParty, Listener}

/*
  * OfficePartyHall is the main actor of the system.
  * It is responsible for creating the OfficeParty actor and handling the communication
  * between the DiYei and the listeners.
  * It also handles the input from the user to interact with the DiYei.
  * The user can propose a song, upvote a song, or downvote a song.
  * The user can also exit the system by typing "exit".
 */
object OfficePartyHall {
  /* The apply method creates the OfficePartyHall actor
   * The OfficeParty actor is created and a message is sent to it to start playing
   * The SessionHandler actor is created to handle the communication between the OfficeParty and the user
   */
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val officeParty = context.spawn(OfficeParty(), "officeParty")
      val handlerRef = context.spawn(SessionHandler(), "handler")
      officeParty ! JoinParty(handlerRef)
      Behaviors.empty

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(OfficePartyHall(), "OfficePartyHall")
  }

  private def SessionHandler(): Behavior[PartySessionEvent] = {
    Behaviors.receive { (context, message) =>
      message match {
        case NewDiYeiAccepted(diyei) =>
          context.log.info("Accepted as a diyei")
          diyei ! DiYei.ShowCommands()
          while (true) {
            val input = scala.io.StdIn.readLine()
            if (input == "exit") {
              context.log.info("Exiting")
              return Behaviors.stopped
            }
            diyei ! DiYei.ProcessCommand(input)
          }
          Behaviors.same
        case NewListenerAccepted(listener) =>
          context.log.info("Accepted as a listener")
          listener ! Listener.ShowCommands()
          while (true) {
            val input = scala.io.StdIn.readLine()
            if (input == "exit") {
              context.log.info("Exiting")
              return Behaviors.stopped
            }
            listener ! Listener.ProcessCommand(input)
          }
          Behaviors.same
        case JoinDenied(reason) =>
          context.log.info(s"Listener denied: $reason")
          Behaviors.stopped
        case _ =>
          Behaviors.same
      }
    }
  }
}