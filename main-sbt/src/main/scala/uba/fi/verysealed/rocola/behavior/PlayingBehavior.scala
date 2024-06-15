package uba.fi.verysealed.rocola.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import uba.fi.verysealed.rocola.RocolaManager._
import uba.fi.verysealed.rocola.behavior.playlist.SongMetadataImporter.importSongsFromJson
import uba.fi.verysealed.rocola.behavior.playlist._

import scala.concurrent.ExecutionContext

object Listener {
  def apply(): Behavior[SongPlaybackEvent] = Behaviors.receive { (context, message) =>
    message match {
      case NewSongStarted(song) =>
        context.log.info(s"Listener received: New song '${song.title}' by ${song.artist} has started.")
        Behaviors.same

      case SongHasFinished(songTitle) =>
        context.log.info(s"Listener received: Song '$songTitle' has finished.")
        Behaviors.same
      case SongEnqueued(song) =>
        context.log.info(s"Listener received: Song '${song.title}' by ${song.artist} has been enqueued.")
        Behaviors.same
    }
  }
}
case class SongMetadata(title: String, artist: String, genre: String, duration: Int, votes:Int = 0)
// Define the JSON format for SongMetadata
object SongMetadata {
  implicit val format: RootJsonFormat[SongMetadata] = jsonFormat5(SongMetadata.apply)
}

class PlayingBehavior(context: ActorContext[RocolaCommand]
                     ) extends AbstractBehavior[RocolaCommand](context) {

  implicit val ec: ExecutionContext = context.system.executionContext
  implicit val materializer: Materializer = Materializer(context.system)

  val songs = importSongsFromJson("default_playlist.json")
  println(s"Imported songs: $songs")
  // Create the classic ActorSystem from the typed ActorSystem
  // Create the Listener actor
  val listener: ActorRef[SongPlaybackEvent] = context.spawn(Listener(), "listener")
  val songsPlayer = new SongPlayer(context, listener)

  // Method to search for a song by title and artist
  def searchSong(title: String, artist: String): Option[SongMetadata] = {
    songs.find(song => song.title.equalsIgnoreCase(title) && song.artist.equalsIgnoreCase(artist))
  }
  override def onMessage(msg: RocolaCommand): Behavior[RocolaCommand] = {
    msg match {
      case SendPlay(replyTo) =>
        try {
          val result = songsPlayer.resumeCurrentSong()
          val message = if (result) "Playing has been started!" else "There is nothing to play!"
          replyTo ! PlaybackResponse(success = result, message = message)
        } catch {
          case e: Exception =>
            replyTo ! PlaybackResponse(success = false, message = s"Failed to play: ${e.getMessage}")
        }
        this

      case SendPause(replyTo) =>
        try {
          val result = songsPlayer.stopCurrentSong()
          val message = if (result) "Successfully paused" else "Player was already paused."
          replyTo ! PlaybackResponse(success = result, message = message)
        } catch {
          case e: Exception =>
            replyTo ! PlaybackResponse(success = false, message = s"Failed to pause: ${e.getMessage}")
        }
        this

      case SendStop(replyTo) =>
        try {
          val result = songsPlayer.stopCurrentSong()
          val message = if (result) "Successfully stopped" else "Player was already stopped."
          replyTo ! PlaybackResponse(success = result, message = message)
        } catch {
          case e: Exception =>
            replyTo ! PlaybackResponse(success = false, message = s"Failed to stop: ${e.getMessage}")
        }
        this

      case SendSkipSong(replyTo) =>
        try {
          songsPlayer.skipCurrentSong()
          replyTo ! PlaybackResponse(success = true, message = "Skipped")
        } catch {
          case e: Exception =>
            replyTo ! PlaybackResponse(success = false, message = s"Failed to skip: ${e.getMessage}")
        }
        this

      case SendVolumeUp(replyTo) =>
        try {
          songsPlayer.increaseVolume(10)
          replyTo ! VolumeControlResponse(success = true, message = "Volume increased")
        } catch {
          case e: Exception =>
            replyTo ! VolumeControlResponse(success = false, message = s"Failed to increase volume: ${e.getMessage}")
        }
        this

      case SendVolumeDown(replyTo) =>
        try {
          songsPlayer.decreaseVolume(10)
          replyTo ! VolumeControlResponse(success = true, message = "Volume decreased")
        } catch {
          case e: Exception =>
            replyTo ! VolumeControlResponse(success = false, message = s"Failed to decrease volume: ${e.getMessage}")
        }
        this

      case SendMute(replyTo) =>
        try {
          songsPlayer.mute()
          replyTo ! VolumeControlResponse(success = true, message = "Muted")
        } catch {
          case e: Exception =>
            replyTo ! VolumeControlResponse(success = false, message = s"Failed to mute: ${e.getMessage}")
        }
        this

      case SendUnmute(replyTo) =>
        try {
          songsPlayer.unMute()
          replyTo ! VolumeControlResponse(success = true, message = "Unmuted")
        } catch {
          case e: Exception =>
            replyTo ! VolumeControlResponse(success = false, message = s"Failed to unmute: ${e.getMessage}")
        }
        this

      case SetVolume(volume, replyTo) =>
        try {
          songsPlayer.setVolume(volume)
          replyTo ! VolumeControlResponse(success = true, message = s"Volume set to $volume")
        } catch {
          case e: Exception =>
            replyTo ! VolumeControlResponse(success = false, message = s"Failed to set volume: ${e.getMessage}")
        }
        this

      case AskPlaylist(replyTo) =>
        try {
          replyTo ! PlaylistResponse(success = true, message = "Playlist obtained", playlist = songsPlayer.getCurrentPlaylist)
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(success = false, message = s"Failed to get playlist: ${e.getMessage}", playlist = songsPlayer.getCurrentPlaylist)
        }
        this

      case SetPlaylist(playlist, replyTo) =>
        try {
          val result = songsPlayer.addNewPlaylist(playlist)
          replyTo ! PlaylistResponse(success = result, message = "Playlist replaced", playlist = playlist)
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(success = false, message = s"Failed to set playlist: ${e.getMessage}", playlist = playlist)
        }
        this

      case DequeueSong(title, artist, replyTo) =>
        try {
          val searchedSong = searchSong(title, artist)
          searchedSong match {
            case Some(song) =>
              println(s"Found song: $song")
              songsPlayer.removeSong(title, artist)
              replyTo ! PlaylistResponse(
                success = true,
                message = s"Song '$title' by $artist has been removed",
                playlist = songsPlayer.getCurrentPlaylist)
            case None =>
              println("Song not found")
              replyTo ! PlaylistResponse(
                success = false,
                message = s"Song not found: $title by $artist",
                playlist = songsPlayer.getCurrentPlaylist)
          }
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(
              success = false,
              message = s"Failed when trying to remove song from queue: $title by $artist. Error: ${e.getMessage}",
              playlist = songsPlayer.getCurrentPlaylist)
        }
        this

      case EnqueueSong(title: String, artist: String, votes: Integer, replyTo) =>
        try {
          val searchedSong = searchSong(title, artist)
          searchedSong match {
            case Some(song) =>
              println(s"Found song: $song")
              val result = songsPlayer.addSong(song)
              val message = if (result) s"Successfully added song: $title by $artist" else s"Song already in queue: $title by $artist"
              replyTo ! PlaylistResponse(result, message, playlist = songsPlayer.getCurrentPlaylist)
            case None =>
              println("Song not found")
              replyTo ! PlaylistResponse(
                success = false,
                s"Specified song has not included into the library yet: $title by $artist",
                playlist = songsPlayer.getCurrentPlaylist)
          }
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(
              success = false,
              message = s"Failed when trying to add a song to the queue: $title by $artist. Error: ${e.getMessage}",
              playlist = songsPlayer.getCurrentPlaylist)
        }
        this
      case DequeueSongByOrdinal(ordinal, replyTo) =>
        try {
          songsPlayer.removeSongByPosition(ordinal)
          val message =s"Song with ordinal $ordinal has been removed"
          replyTo ! PlaylistResponse(success = true, message, playlist = songsPlayer.getCurrentPlaylist)
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(success = false, message = s"Failed to remove song with ordinal $ordinal: ${e.getMessage}", playlist = songsPlayer.getCurrentPlaylist)
        }
        this
      case VoteSong(votePositive, title, artist, replyTo) =>
        try {
          val quantity = if (votePositive) 1 else -1
          songsPlayer.updateVotes(title, artist, quantity)
          replyTo ! PlaylistResponse(success = true, message = s"Vote for song $title by $artist has been updated", playlist = songsPlayer.getCurrentPlaylist)
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(success = false, message = s"Failed to update votes for song $title by $artist: ${e.getMessage}", playlist = songsPlayer.getCurrentPlaylist)
        }
        this

      case VoteSongByOrdinal(votePositive, ordinal, replyTo) =>
        try {
          val quantity = if (votePositive) 1 else -1
          songsPlayer.updateVotesByPosition(ordinal, quantity)
          replyTo ! PlaylistResponse(success = true, message = s"Vote for song with ordinal $ordinal has been updated", playlist = songsPlayer.getCurrentPlaylist)
        } catch {
          case e: Exception =>
            replyTo ! PlaylistResponse(success = false, message = s"Failed to update votes for song with ordinal $ordinal: ${e.getMessage}", playlist = songsPlayer.getCurrentPlaylist)
        }
        this
    }
  }
}
