package uba.fi.verysealed.rocola.routes

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import spray.json.DefaultJsonProtocol.jsonFormat3
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, RootJsonFormat}
import uba.fi.verysealed.rocola.SessionManager
import uba.fi.verysealed.rocola.SessionManager._
import uba.fi.verysealed.rocola.behavior.SongMetadata

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.Future
import scala.concurrent.duration._

object RegisterRouteHandler extends DefaultJsonProtocol {
  // Custom JSON format for MemberType
  implicit object MemberTypeFormat extends JsonFormat[MemberType] {
    def write(memberType: MemberType): JsValue = memberType match {
      case DiYei   => JsString("DiYei")
      case Listener => JsString("Listener")
    }
    def read(value: JsValue): MemberType = value match {
      case JsString("DiYei")   => DiYei
      case JsString("Listener") => Listener
      case _ => throw new DeserializationException("MemberType expected")
    }
  }


  implicit val memberFormat: RootJsonFormat[Member] = jsonFormat4(Member)
  implicit val membersListResponseFormat: RootJsonFormat[MembersListResponse] = jsonFormat1(MembersListResponse)
  implicit val revocationResponseFormat: RootJsonFormat[RevocationResponse] = jsonFormat2(RevocationResponse)

  // Define the JSON response case class
  case class RegistrationResponseMessage(message: String, success: Boolean, token: String)

  // Define the JSON format for the response
  object RegistrationResponseMessage {
    implicit val format: RootJsonFormat[RegistrationResponseMessage] = jsonFormat3(RegistrationResponseMessage.apply)
  }

  case class RegisterRequest(name: String, ip: String, port: Int)
  implicit val registerRequestFormat: RootJsonFormat[RegisterRequest] = jsonFormat3(RegisterRequest)


  def apply(sessionManager: ActorRef[SessionManager.Command])(implicit system: ActorSystem[_]): Route = {
    implicit val timeout: Timeout = 5.seconds

    path("register-diyei") {
      post {
        entity(as[RegisterRequest]) { request =>
          val responseFuture: Future[RegistrationResponse] = sessionManager.ask(ref => RegisterDiYei(request.name, request.ip, request.port, ref))
          onSuccess(responseFuture) { response =>
            if (response.success){
              complete(RegistrationResponseMessage(response.message, response.success,response.token.get ))
            }
            else
              {
                complete(RegistrationResponseMessage(response.message, response.success,"" ))
              }
          }
        }
      }
    } ~
      path("register-listener") {
        post {
          entity(as[RegisterRequest]) { request =>
            val responseFuture: Future[RegistrationResponse] = sessionManager.ask(ref => RegisterListener(request.name, request.ip, request.port, ref))
            onSuccess(responseFuture) { response =>
              if (response.success){
                complete(RegistrationResponseMessage(response.message, response.success,"" ))
              }
              else
              {
                complete(RegistrationResponseMessage(response.message, response.success,"" ))
              }
            }
          }
        }
      } ~
      path("list-members") {
        get {
          val responseFuture: Future[MembersListResponse] = sessionManager.ask(ref => ListMembers(ref))
          onSuccess(responseFuture) { response =>
            complete(response)
          }
        }
      }
  }
}
