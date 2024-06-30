package uba.fi.peerdy.actors


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import spray.json._
import uba.fi.peerdy.actors.DiYei.DiYeiCommand
import uba.fi.peerdy.actors.Listener.ListenerCommand
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri, HttpEntity, ContentTypes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}


case class Member(name: String, ip: String, port: Int, memberType: String)
case class RegisterRequest(name: String, ip: String, port: Int)
case class RegistrationResponseMessage(message: String, success: Boolean, token: String)

trait JsonSupport extends DefaultJsonProtocol {
  implicit val memberFormat: RootJsonFormat[Member] = jsonFormat4(Member)
  implicit val registerRequestFormat: RootJsonFormat[RegisterRequest] = jsonFormat3(RegisterRequest)
  implicit val registrationResponseMessageFormat: RootJsonFormat[RegistrationResponseMessage] = jsonFormat3(RegistrationResponseMessage)

}

/*
 * OfficeParty handles a specific party session once started.
 * It is responsible for creating the DiYei actor and handling the communication
 * between the DiYei and the listeners.
 */
object OfficeParty extends JsonSupport {

  sealed trait PartyCommand
  final case class JoinParty(replyTo: ActorRef[PartySessionEvent]) extends PartyCommand
  final case class RegisterAsDiYei(replyTo: ActorRef[PartySessionEvent]) extends PartyCommand
  final case class RegisterAsListener(replyTo: ActorRef[PartySessionEvent], diyei: Member) extends PartyCommand
  final case class HandleListMembersResponse(replyTo: ActorRef[PartySessionEvent], response: Try[String]) extends PartyCommand
  final case class HandleRegisterDiYeiResponse(replyTo: ActorRef[PartySessionEvent], response: Try[String]) extends PartyCommand
  final case class HandleRegisterListenerResponse(replyTo: ActorRef[PartySessionEvent], diyei: Member, response: Try[String]) extends PartyCommand

  sealed trait PartySessionEvent
  final case class NewDiYeiAccepted(handle: ActorRef[DiYeiCommand]) extends PartySessionEvent
  final case class NewListenerAccepted(handle: ActorRef[ListenerCommand]) extends PartySessionEvent
  final case class JoinDenied(reason: String) extends PartySessionEvent

  
  // using FP to define behavior as a function in this object
  def apply(address: String, clientPort: Int, name: String): Behavior[PartyCommand] = {
    Behaviors.receive { (context, message) =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      implicit val directory:String = "localhost"
      implicit val port:String = "4545"
      val http = Http(system)

      message match {
        case JoinParty(client) =>
          val uri = Uri(s"http://$directory:$port/list-members")
          val request = HttpRequest(method = HttpMethods.GET, uri = uri)

          // Send HTTP request and pipe response to self
          context.pipeToSelf(http.singleRequest(request).flatMap { response =>
            Unmarshal(response.entity).to[String]
          }) {
            case Success(value) => HandleListMembersResponse(client, Success(value))
            case Failure(exception) => HandleListMembersResponse(client, Failure(exception))
          }
          Behaviors.same
        
        case RegisterAsDiYei(replyTo) =>
          val uri = Uri(s"http://$directory:$port/register-diyei")
          val registerRequest = RegisterRequest(name, address, clientPort)
          val entity = HttpEntity(ContentTypes.`application/json`, registerRequest.toJson.toString)
          
          val request = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          context.pipeToSelf(responseFuture.flatMap { response =>
            Unmarshal(response.entity).to[String]
          }) {
            case Success(value) => HandleRegisterDiYeiResponse(replyTo, Success(value))
            case Failure(exception) => HandleRegisterDiYeiResponse(replyTo, Failure(exception))
          }
          Behaviors.same

        case RegisterAsListener(replyTo, diyei) =>
          val uri = Uri(s"http://$directory:$port/register-listener")
          val registerRequest = RegisterRequest(name, address, clientPort)
          val entity = HttpEntity(ContentTypes.`application/json`, registerRequest.toJson.toString)
          
          val request = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)
          val responseFuture: Future[HttpResponse] = http.singleRequest(request)

          context.pipeToSelf(responseFuture.flatMap { response =>
            Unmarshal(response.entity).to[String]
          }) {
            case Success(value) => HandleRegisterListenerResponse(replyTo, diyei, Success(value))
            case Failure(exception) => HandleRegisterListenerResponse(replyTo, diyei, Failure(exception))
          }
          Behaviors.same

        // ListMembers response
        case HandleListMembersResponse(replyTo, Success(value)) =>
          val members = value.parseJson.asJsObject.fields("members").convertTo[List[Member]]
          val diYei = members.find(_.memberType == "DiYei")

          diYei match {
            case Some(diYei) =>
              context.self ! RegisterAsListener(replyTo, diYei)
              Behaviors.same
            case None =>
              context.self ! RegisterAsDiYei(replyTo)
              Behaviors.same
          }
        case HandleListMembersResponse(replyTo, Failure(exception)) =>
          // Queda colgado. TODO: Handlear el error
          context.log.error(s"Failed to fetch users: ${exception.getMessage}")
          Behaviors.same
        
        // RegisterDiYei Response
        case HandleRegisterDiYeiResponse(replyTo, Success(value)) =>
          val registrationResponse = value.parseJson.convertTo[RegistrationResponseMessage]
          registrationResponse.success match {
            case true =>
              val diYei = context.spawn(DiYei(address, clientPort), "diyei")
              replyTo ! NewDiYeiAccepted(diYei)
              Behaviors.same
            case false =>
              context.log.info("Could not register as a DiYei")
              Behaviors.same
          }
        case HandleRegisterDiYeiResponse(replyTo, Failure(exception)) =>
          context.log.info(s"Failed to process response: $exception")
          Behaviors.same

        // RegisterListener Response
        case HandleRegisterListenerResponse(replyTo, diyei, Success(value)) =>
          val registrationResponse = value.parseJson.convertTo[RegistrationResponseMessage]
          registrationResponse.success match {
            case true =>
              val listener = context.spawn(Listener(address, clientPort, diyei), "listener")
              replyTo ! NewListenerAccepted(listener)
              Behaviors.same
            case false =>
              context.log.info("Could not register as a listener")
              Behaviors.same
          }
        case HandleRegisterListenerResponse(replyTo, diyei, Failure(exception)) =>
          context.log.info(s"Failed to process response: $exception")
          Behaviors.same
      }
    }
  }
}


