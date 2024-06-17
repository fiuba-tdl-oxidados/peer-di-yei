package uba.fi.verysealed.rocola.behavior.playlist

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.stream.{KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, Source, SourceQueueWithComplete}
import uba.fi.verysealed.rocola.RocolaManager.RocolaCommand
import uba.fi.verysealed.rocola.behavior.SongMetadata

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.collection.mutable.PriorityQueue

sealed trait SongPlaybackEvent
final case class SongHasFinished(songTitle: String) extends SongPlaybackEvent
final case class NewSongStarted(song: SongMetadata) extends SongPlaybackEvent
final case class CurrentSongEnded(song: SongMetadata) extends SongPlaybackEvent
final case class CurrentSongStopped(song: SongMetadata) extends SongPlaybackEvent
final case class CurrentSongResumed(song: SongMetadata) extends SongPlaybackEvent
final case class CurrentSongSkipped(song: SongMetadata) extends SongPlaybackEvent
final case class SongEnqueued(song: SongMetadata) extends SongPlaybackEvent
final case class PlaybackStatusChanged(message: String) extends SongPlaybackEvent
final case class PlaylistStatusChanged(message: String) extends SongPlaybackEvent

class SongPlayer(context: ActorContext[RocolaCommand], listener: ActorRef[SongPlaybackEvent], queue: SourceQueueWithComplete[PlaybackStatusChanged]) {
  implicit val ec: ExecutionContext = context.system.executionContext
  implicit val materializer: Materializer = Materializer(context.system)

  private var songQueue: mutable.PriorityQueue[SongMetadata] = mutable.PriorityQueue.empty(Ordering.by(_.votes)) // Default ordering by votes
  private var currentSong: Option[SongMetadata] = None
  private var elapsedTime: Option[AtomicInteger] = None

  private var currentSongSource: Option[akka.stream.scaladsl.Source[String, _]] = None
  private var songSourceControl: Option[akka.stream.KillSwitch] = None

  private val minVolume = 0
  private val maxVolume = 100
  private val volume: AtomicInteger = new AtomicInteger(50) // Default volume is 50
  private val muted: AtomicBoolean = new AtomicBoolean(false)

  private def publishStatus(message: String): Unit = {
    val status = PlaybackStatusChanged(message)
    queue.offer(status)
    listener ! status
  }

  def increaseVolume(step: Int): Unit = {
    val newVolume = Math.min(maxVolume, volume.addAndGet(step))
    volume.set(newVolume)
    listener ! PlaybackStatusChanged(s"Volume increased to $newVolume")
  }

  def mute(): Unit = {
    muted.set(true)
    listener ! PlaybackStatusChanged("Volume muted")
  }

  def unMute(): Unit = {
    muted.set(false)
    listener ! PlaybackStatusChanged("Volume unmuted")
  }
  def skipCurrentSong(): Unit = {
    songSourceControl.foreach(_.shutdown())
    currentSong match {
      case Some(song) =>
        listener ! CurrentSongSkipped(song)
        listener ! PlaybackStatusChanged(s"Skipped song '${song.title}' by '${song.artist}'")
        currentSong = None
        songSourceControl = None
        playNextSong()
      case None =>
        listener ! PlaybackStatusChanged("No song is currently playing to skip")
    }
  }
  def decreaseVolume(step: Int): Unit = {
    val newVolume = Math.max(minVolume, volume.addAndGet(-step))
    volume.set(newVolume)
    listener ! PlaybackStatusChanged(s"Volume decreased to $newVolume")
  }
  // Helper function to format elapsed time in minutes and seconds
  private def formatTime(seconds: Int): String = {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    f"$minutes%02d:$remainingSeconds%02d"
  }

  private def createSongSource(song: SongMetadata, startFrom: Int = 0): Source[String, _] = {
    val elapsed = new AtomicInteger(startFrom)
    elapsedTime = Some(elapsed)

    val (killSwitch, tickSource) = Source.tick(0.seconds, 5.seconds, 5)
      .scan(startFrom)((counter, step) => counter + step) // Increment the counter
      .takeWhile(_ <= song.duration) // Stop the source when the counter reaches the song duration
      .map { seconds =>
        elapsed.set(seconds)
        val message = s"Playing song ${song.title} by ${song.artist}, elapsed ${formatTime(seconds)}"
        publishStatus(message)
        message
      } // Format the message and send playback status
      .viaMat(KillSwitches.single)(Keep.right)
      .preMaterialize()

    songSourceControl = Some(killSwitch)

    val endOfSongMessage = Source.single {
      listener ! SongHasFinished(song.title) // Send a message to the Listener actor
      s"Finished playing song ${song.title}"
    }

    currentSongSource = Some(tickSource.concat(endOfSongMessage)) // Concatenate the end-of-song message source
    tickSource.concat(endOfSongMessage)
  }

  def stopCurrentSong(): Boolean = {
    songSourceControl.foreach(_.shutdown())
    currentSong match {
      case Some(song) =>
        listener ! CurrentSongStopped(song)
        listener ! PlaybackStatusChanged(s"Stopped playing song '${song.title}' by '${song.artist}'")
        currentSong = None
        songSourceControl = None
        true
      case None =>
        listener ! PlaybackStatusChanged("No song is currently playing")
        false
    }
  }

  def setVolume(volumeLevel: Int): Unit = {
    if (volumeLevel < minVolume || volumeLevel > maxVolume) {
      throw new OutOfBoundException(s"Volume level $volumeLevel is out of bounds (must be between $minVolume and $maxVolume)")
    } else {
      volume.set(volumeLevel)
      listener ! PlaybackStatusChanged(s"Volume set to $volumeLevel")
    }
  }

  def resumeCurrentSong(): Boolean = {
    currentSong match {
      case Some(song) if elapsedTime.isDefined =>
        val startFrom = elapsedTime.get.get()
        listener ! CurrentSongResumed(song)
        listener ! PlaybackStatusChanged(s"Resuming song '${song.title}' by '${song.artist}' from ${formatTime(startFrom)}")

        val songSource = createSongSource(song, startFrom)
        songSource.runForeach(message => println(message)).onComplete { _ =>
          currentSong = None
          listener ! CurrentSongEnded(song)
          playNextSong()
        }
        true
      case _ =>
        listener ! PlaybackStatusChanged("No song to resume, trying to play next song, if defined...")
        playNextSong()
    }
  }

  def addSong(song: SongMetadata): Boolean = {
    songQueue.enqueue(song)
    listener ! SongEnqueued(song)
    true
  }

  private def playNextSong(): Boolean = {
    if (currentSong.isEmpty && songQueue.nonEmpty) {
      val song = songQueue.dequeue()
      currentSong = Some(song)
      listener ! NewSongStarted(song)

      val songSource = createSongSource(song)
      songSource.runForeach(message => println(message)).onComplete { _ =>
        currentSong = None
        listener ! CurrentSongEnded(song)
        playNextSong()
      }
      true
    }
    else
      false
  }

  private def rankQueue(ranking: (SongMetadata, SongMetadata) => Boolean): Unit = {
    songQueue = mutable.PriorityQueue(songQueue.toSeq: _*)(Ordering.fromLessThan(ranking))
  }


  def updateVotes(songTitle: String, artist: String, quantity: Int): Unit = {
    val songOpt = songQueue.find(song => song.title == songTitle && song.artist == artist)
    songOpt match {
      case Some(song) =>
        songQueue = songQueue.filterNot(_ == song)
        val updatedVotes = math.max(0, song.votes + quantity)
        val updatedSong = song.copy(votes = updatedVotes)
        songQueue.enqueue(updatedSong)
        listener ! PlaylistStatusChanged(s"Votes for song '${song.title}' by '${song.artist}' updated to $updatedVotes")
        rankQueue((a, b) => a.votes > b.votes) // Re-rank the queue
      case None =>
        listener ! PlaylistStatusChanged(s"Song '$songTitle' by '$artist' not found in queue")
    }
  }
  def updateVotesByPosition(position: Int, quantity: Int): Unit = {
    if (position < 0 || position >= songQueue.size) {
      listener ! PlaybackStatusChanged(s"Invalid position: $position")
      return
    }
    val songs = songQueue.toList
    val song = songs(position)
    songQueue = mutable.PriorityQueue(songs.filterNot(_ == song): _*)(Ordering.by(_.votes))
    val updatedVotes = math.max(0, song.votes + quantity)
    val updatedSong = song.copy(votes = updatedVotes)
    songQueue.enqueue(updatedSong)
    listener ! PlaybackStatusChanged(s"Votes for song '${song.title}' by '${song.artist}' updated to $updatedVotes")
  }

  def removeSong(title: String, artist: String): Unit = {
    val songOpt = songQueue.find(song => song.title == title && song.artist == artist)
    songOpt match {
      case Some(song) =>
        songQueue = songQueue.filterNot(_ == song)
        listener ! PlaybackStatusChanged(s"Removed song '${song.title}' by '${song.artist}' from the queue")
      case None =>
        listener ! PlaybackStatusChanged(s"Song '$title' by '$artist' not found in queue")
    }
  }
  def removeSongByPosition(position: Int): Unit = {
    if (position < 0 || position >= songQueue.size) {
      listener ! PlaybackStatusChanged(s"Invalid position: $position")
      return
    }
    val songs = songQueue.toList
    val song = songs(position)
    songQueue = mutable.PriorityQueue(songs.filterNot(_ == song): _*)(Ordering.by(_.votes))
    listener ! PlaybackStatusChanged(s"Removed song '${song.title}' by '${song.artist}' from the queue at position $position")
  }

  def getCurrentPlaylist: List[SongMetadata] = {
    songQueue.toList
  }

  def addNewPlaylist(songs: List[SongMetadata]): Boolean = {
    songQueue.clear()
    songs.foreach(songQueue.enqueue(_))
    rankQueue((a, b) => a.votes > b.votes) // Order the queue by votes
    listener ! PlaybackStatusChanged("New playlist added and ordered by votes")
    true
  }
}
