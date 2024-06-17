package uba.fi.verysealed.rocola

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import spray.json._

object SessionManager {
  sealed trait Command
  final case class RegisterDiYei(name: String, ip: String, port: Int, replyTo: ActorRef[RegistrationResponse]) extends Command
  final case class RegisterListener(name: String, ip: String, port: Int, replyTo: ActorRef[RegistrationResponse]) extends Command
  final case class ListMembers(replyTo: ActorRef[MembersListResponse]) extends Command
  final case class RevokeDiYei(replyTo: ActorRef[RevocationResponse]) extends Command

  case class RegistrationResponse(success: Boolean, message: String, token: Option[String] = None)
  case class MembersListResponse(members: List[Member])
  case class RevocationResponse(success: Boolean, message: String)

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

  private def generateToken(): String = java.util.UUID.randomUUID().toString

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case RegisterDiYei(name, ip, port, replyTo) =>
        diyei match {
          case Some(_) =>
            replyTo ! RegistrationResponse(success = false, message = "DiYei already registered")
          case None =>
            val token = generateToken()
            diyei = Some(Member(name, ip, port, DiYei))
            replyTo ! RegistrationResponse(success = true, message = "DiYei registered", token = Some(token))
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

      case RevokeDiYei(replyTo) =>
        diyei match {
          case Some(_) =>
            diyei = None
            replyTo ! RevocationResponse(success = true, message = "DiYei registration revoked")
          case None =>
            replyTo ! RevocationResponse(success = false, message = "No DiYei registered")
        }
        Behaviors.same
    }
  }
}
