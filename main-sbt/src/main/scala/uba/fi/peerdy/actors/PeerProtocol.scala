package uba.fi.peerdy.actors

object PeerProtocol {
  sealed trait DirectoryCommand
  final case class StartSession() extends DirectoryCommand
  final case class EndSession() extends DirectoryCommand
  final case class GetPeers() extends DirectoryCommand
  
  sealed trait SessionCommand
  final case class SessionStarted() extends SessionCommand
  final case class SessionEnded() extends SessionCommand
  final case class SessionDenied() extends SessionCommand
  final case class PeerListPublished() extends SessionCommand
  
  sealed trait PeerCommand
  final case class PeerJoined() extends PeerCommand
  final case class PeerLeft() extends PeerCommand
  final case class PeerUpdated() extends PeerCommand

  sealed trait CommunicationCommand
  final case class SendMessage() extends CommunicationCommand
  final case class ReceiveMessage() extends CommunicationCommand
  final case class BroadcastMessage() extends CommunicationCommand
  
  sealed trait MessageCommand
  final case class MessageReceived() extends CommunicationCommand
  final case class MessageSent() extends CommunicationCommand
  final case class MessagePublished() extends CommunicationCommand
  

}
