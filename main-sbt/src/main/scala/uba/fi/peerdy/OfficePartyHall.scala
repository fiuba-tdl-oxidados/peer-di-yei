package uba.fi.peerdy

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json._
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}
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
object OfficePartyHall extends JsonSupport {

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
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      implicit val directory:String = "localhost"
      implicit val port:String = "4545"
      val http = Http(system)

      message match {
        case NewDiYeiAccepted(handle) =>
          context.log.info("DiYei accepted")
          diyeiProxy = Option(handle)
          commandList()
          while (true) {
            val input = scala.io.StdIn.readLine()
            if (input == "exit") {
              context.log.info("Exiting")
              return Behaviors.stopped
            }
            translateDiYeiCommand(input)
          }
          Behaviors.same
        case NewDiYeiDenied(replyTo, reason) =>
          val uri = Uri(s"http://$directory:$port/list-members")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)

          // Send HTTP request and pipe response to self
          context.pipeToSelf(http.singleRequest(request).flatMap { response =>
            Unmarshal(response.entity).to[String]
          }) {
            case Success(value) => WrappedHttpResponse(replyTo, Success(value))
            case Failure(exception) => WrappedHttpResponse(replyTo, Failure(exception))
          }
          Behaviors.same
        case NewListenerAccepted(handle) =>
          //TODO: implement here the Listener behavior
          context.log.info("Listener accepted")
          Behaviors.same
        case NewListenerDenied(reason) =>
          context.log.info(s"Listener denied: $reason")
          Behaviors.stopped
        case WrappedHttpResponse(replyTo, Success(value)) =>
            context.log.info(s"HTTP response received: $value")

            // Parse the JSON response to extract users
            val members = value.parseJson.convertTo[List[Member]]

            // Find the DiYei from the users list
            val diYei = members.find(_.role == "DiYei")

            diYei match {
              case Some(diYei) =>
                context.log.info(s"DiYei found: ${diYei.name}")
                // Launch the StartListening event
                replyTo ! StartListening("TODO:NAME", context.self.asInstanceOf[ActorRef[PartySessionEvent]])
              case None =>
                context.log.warn("No DiYei found in the user list")
            }

            Behaviors.same
          case WrappedHttpResponse(replyTo, Failure(exception)) =>
            context.log.error(s"Failed to fetch users: ${exception.getMessage}")
            Behaviors.same
        case _ =>
          Behaviors.same
      }
    }
  }

  //TODO: implement here full implementation for handing interaction with user as a Diyei
  private def translateDiYeiCommand(str: String): Unit = {
    str match {
      case "commands" =>
        commandList()
      case "propose" =>
        println("Enter song name:")
        val song = scala.io.StdIn.readLine()
        println("Enter artist name:")
        val artist = scala.io.StdIn.readLine()
        println(s"Proposing song: $song from artist: $artist")
        diyeiProxy.get ! DiYei.ProposeSong("DiYei",song,artist)
      case "upvote" =>
        println("Enter song name: ")
        val song: String = scala.io.StdIn.readLine()
        diyeiProxy.get ! DiYei.UpVoteSong(song)
        println("Upvoting song")
      case "downvote" =>
        println("Enter song name: ")
        val song: String = scala.io.StdIn.readLine()
        diyeiProxy.get ! DiYei.DownVoteSong(song)
        println("Downvoting song")
      case _ =>
        println("Invalid command")
    }
  }

  private def commandList(): Unit = {
    println("Available commands:")
    println("propose - Propose a song")
    println("upvote - Upvote a song")
    println("downvote - Downvote a song")
    println("commands - List available commands")
    println("exit - Exit the system")
  }

}