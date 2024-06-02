package uba.fi.peerdy.actors

object Roccola {

  sealed trait SessionCommand
  final case class StartSession() extends SessionCommand

  sealed trait SessionEvents
  final case class SessionStarted() extends SessionEvents
  final case class SessionEnded() extends SessionEvents
  final case class SessionDenied() extends SessionEvents

  sealed trait RoccolaCommand
  final case class Play() extends RoccolaCommand
  final case class Pause() extends RoccolaCommand
  final case class Stop() extends RoccolaCommand
  final case class Skip() extends RoccolaCommand
  final case class VolumeUp() extends RoccolaCommand
  final case class VolumeDown() extends RoccolaCommand
  final case class Mute() extends RoccolaCommand
  final case class Unmute() extends RoccolaCommand
  final case class SetVolume(volume: Int) extends RoccolaCommand
  final case class SetPlaylist() extends RoccolaCommand
  
  sealed trait SongCommand
  final case class NewSongStarted()
  final case class CurrentSongEnded()

}
