package uba.fi.peerdy

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import uba.fi.peerdy.actors.DiYei.DiYeiCommand
import uba.fi.peerdy.actors.OfficeParty._
import uba.fi.peerdy.actors.{DiYei, OfficeParty}

/*
  * OfficePartyHall is the main actor of the system.
  * It is responsible for creating the OfficeParty actor and handling the communication
  * between the DiYei and the listeners.
  * It also handles the input from the user to interact with the DiYei.
  * The user can propose a song, upvote a song, or downvote a song.
  * The user can also exit the system by typing "exit".
 */
object OfficePartyHall {

  // The DiYei actor reference wrapped in an Option to handle the case when the DiYei is not available
  private var diyeiProxy:  Option[ActorRef[DiYeiCommand]] = Option.empty

  /* The apply method creates the OfficePartyHall actor
   * The OfficeParty actor is created and a message is sent to it to start playing
   * The SessionHandler actor is created to handle the communication between the OfficeParty and the user
   */
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
    ActorSystem(OfficePartyHall(), "OfficePartyHall")
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
          //TODO: implement here the Listener behavior
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

  //TODO: implement here full implementation for handing interaction with user as a Diyei
  private def translateDiYeiCommand(str: String): Unit = {
    str match {
      case "propose" =>
        val song = scala.io.StdIn.readLine()
        val artist = scala.io.StdIn.readLine()
        println(s"Proposing song: $song from artist: $artist")

        println(s"Proposing song: $song")
        diyeiProxy.get ! DiYei.ProposeSong("DiYei",song,artist)
      case "upvote" =>
        println("Upvoting song")
      case "downvote" =>
        println("Downvoting song")
      case _ =>
        println("Invalid command")
    }
  }

}