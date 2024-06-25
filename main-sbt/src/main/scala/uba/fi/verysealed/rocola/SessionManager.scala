package uba.fi.verysealed.rocola

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import spray.json._

object SessionManager {
  sealed trait Command
  final case class RegisterDiYei(name: String, ip: String, port: Int, replyTo: ActorRef[RegistrationResponse]) extends Command
  final case class RegisterListener(name: String, ip: String, port: Int, replyTo: ActorRef[RegistrationResponse]) extends Command
  final case class ListMembers(replyTo: ActorRef[MembersListResponse]) extends Command
  final case class RevokeDiYei(token: String, replyTo: ActorRef[RevocationResponse]) extends Command
  final case class RevokeListener(name: String, replyTo: ActorRef[RevocationResponse]) extends Command
  final case class PromoteDiYei(token: String, name: String, replyTo: ActorRef[RegistrationResponse]) extends Command
  final case class ValidateToken(token: String, replyTo: ActorRef[ValidationResponse]) extends Command

  case class RegistrationResponse(success: Boolean, message: String, token: Option[String] = None)
  case class MembersListResponse(members: List[Member])
  case class RevocationResponse(success: Boolean, message: String)
  case class ValidationResponse(valid: Boolean)

  sealed trait MemberType
  case object DiYei extends MemberType
  case object Listener extends MemberType

  case class Member(name: String, ip: String, port: Int, memberType: MemberType)

  object MemberType {
    implicit val memberTypeFormat: JsonFormat[MemberType] = new JsonFormat[MemberType] {
      def write(obj: MemberType): JsValue = JsString(obj.toString)

      def read(json: JsValue): MemberType = json match {
        case JsString("DiYei") => DiYei
        case JsString("Listener") => Listener
        case _ => throw new DeserializationException("MemberType expected")
      }
    }
  }

  private var diyei: Option[Member] = None
  private var listeners: List[Member] = List.empty
  private var token: Option[String] = None

  private def generateToken(): String = java.util.UUID.randomUUID().toString

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case RegisterDiYei(name, ip, port, replyTo) =>
        diyei match {
          case Some(_) =>
            replyTo ! RegistrationResponse(success = false, message = "DiYei already registered")
          case None =>
            val newToken = generateToken()
            diyei = Some(Member(name, ip, port, DiYei))
            token = Some(newToken)
            replyTo ! RegistrationResponse(success = true, message = "DiYei registered", token = Some(newToken))
        }
        Behaviors.same

      case RegisterListener(name, ip, port, replyTo) =>
        listeners = listeners :+ Member(name, ip, port, Listener)
        replyTo ! RegistrationResponse(success = true, message = "Listener registered")
        Behaviors.same

      case ListMembers(replyTo) =>
        val members = diyei.toList ++ listeners
        replyTo ! MembersListResponse(members)
        Behaviors.same

      case RevokeDiYei(providedToken, replyTo) =>
        if (token.contains(providedToken)) {
          diyei = None
          token = None
          replyTo ! RevocationResponse(success = true, message = "DiYei registration revoked")
        } else {
          replyTo ! RevocationResponse(success = false, message = "Invalid token")
        }
        Behaviors.same

      case RevokeListener(name, replyTo) =>
        listeners.find(_.name == name) match {
          case Some(listener) =>
            listeners = listeners.filterNot(_.name == name)
            replyTo ! RevocationResponse(success = true, message = s"Listener $name unregistered")
          case None =>
            replyTo ! RevocationResponse(success = false, message = s"Listener $name not found")
        }
        Behaviors.same

      case PromoteDiYei(providedToken, name, replyTo) =>
        if (token.contains(providedToken)) {
          listeners.find(_.name == name) match {
            case Some(listener) =>
              val newToken = generateToken()
              // add diyei as a listener
              val previousDiyei = diyei.get
              listeners = listeners:+ Member(previousDiyei.name, previousDiyei.ip, previousDiyei.port, Listener)
              diyei = Some(listener.copy(memberType = DiYei))
              listeners = listeners.filterNot(_.name == name)
              token = Some(newToken)
              replyTo ! RegistrationResponse(success = true, message = s"Listener $name promoted to DiYei", token = Some(newToken))
            case None =>
              replyTo ! RegistrationResponse(success = false, message = s"Listener $name not found")
          }
        } else {
          replyTo ! RegistrationResponse(success = false, message = "Invalid token")
        }
        Behaviors.same

      case ValidateToken(requestToken, replyTo) =>
        val isValid = token.contains(requestToken)
        replyTo ! ValidationResponse(isValid)
        Behaviors.same
    }
  }
}
