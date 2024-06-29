package uba.fi.peerdy

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import uba.fi.peerdy.actors.DiYei._
import uba.fi.peerdy.actors.OfficeParty._
import uba.fi.peerdy.actors.Listener._
import uba.fi.peerdy.actors.{DiYei, OfficeParty, Listener}
import scopt.OParser


case class Config(address: String = "localhost", port: Int = 8888, name: String = "noname")

object ConfigParser {
  val builder = OParser.builder[Config]
  val parser = {
    import builder._
    OParser.sequence(
      programName("OfficePartyHall"),
      head("OfficePartyHall", "0.1"),
      opt[String]('a', "address")
        .action((x, c) => c.copy(address = x))
        .text("server address"),
      opt[Int]('p', "port")
        .action((x, c) => c.copy(port = x))
        .text("server port"),
      opt[String]('n', "name")
        .action((x, c) => c.copy(name = x))
        .text("username")
    )
  }
}
/*
  * OfficePartyHall is the main actor of the system.
  * It is responsible for creating the OfficeParty actor and handling the communication
  * between the DiYei and the listeners.
  * It also handles the input from the user to interact with the DiYei.
  * The user can propose a song, upvote a song, or downvote a song.
  * The user can also exit the system by typing "exit".
  * 
  * 
  * Example: sbt "run --address localhost --port 8080 --name Ariel"
  * 
  * 
 */
object OfficePartyHall {
  /* The apply method creates the OfficePartyHall actor
   * The OfficeParty actor is created and a message is sent to it to start playing
   * The SessionHandler actor is created to handle the communication between the OfficeParty and the user
   */
  def apply(args: Array[String]): Behavior[NotUsed] =
    Behaviors.setup { context =>
      OParser.parse(ConfigParser.parser, args, Config()) match {
        case Some(config) =>
          val officeParty = context.spawn(OfficeParty(config.address, config.port, config.name), "officeParty")
          val handlerRef = context.spawn(SessionHandler(), "handler")
          officeParty ! JoinParty(handlerRef)

          Behaviors.empty

          Behaviors.receiveSignal {
            case (_, Terminated(_)) =>
              Behaviors.stopped
          }
        case _ =>
          // Argumentos de línea de comandos no válidos
          println("Invalid arguments")
          Behaviors.stopped
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(OfficePartyHall(args), "OfficePartyHall")
  }

  private def SessionHandler(): Behavior[PartySessionEvent] = {
    Behaviors.receive { (context, message) =>
      message match {
        case NewDiYeiAccepted(diyei) =>
          println("***********************************************")
          println("**           YOU ARE NOW A DIYEI             **")
          println("***********************************************")
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
          println("***********************************************")
          println("**         YOU ARE NOW A LISTENER            **")
          println("***********************************************")
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