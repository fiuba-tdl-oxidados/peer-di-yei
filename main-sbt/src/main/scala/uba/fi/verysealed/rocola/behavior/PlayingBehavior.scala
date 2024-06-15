package uba.fi.verysealed.rocola.behavior

import akka.Done
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.{Actor, ActorSystem}
import akka.actor.typed.{ActorRef, Behavior, Props}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import uba.fi.verysealed.rocola.RocolaManager.PlaySessionCommand
import uba.fi.verysealed.rocola.RocolaManager._
import uba.fi.verysealed.rocola.behavior.playlist.SongMetadataImporter.importSongsFromJson

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class SongMetadata(title: String, artist: String, genre: String, duration: Int)

class PlayingBehavior(context: ActorContext[RocolaCommand]
                               ) extends AbstractBehavior[RocolaCommand](context) {


  case class SongHasFinished(songTitle: String)

  object Listener {
    def apply(): Behavior[SongHasFinished] = Behaviors.receive { (context, message) =>
      message match {
        case SongHasFinished(songTitle) =>
          context.log.info(s"Listener received: Song '$songTitle' has finished.")
          Behaviors.same
      }
    }
  }
  // Class to manage song playback
  class SongPlayer {
    private var songSources: List[Source[String, _]] = List.empty

    def addSong(song: SongMetadata): Unit = {
      songSources = songSources :+ createSongSource(song)
    }

    def playAllSongs(): Future[Done] = {
      val concatenatedSource = songSources.reduce((acc, src) => acc.concat(src))
      concatenatedSource.runForeach(message => println(message))
    }
  }

  // Helper function to format elapsed time in minutes and seconds
  def formatTime(seconds: Int): String = {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    f"$minutes%02d:$remainingSeconds%02d"
  }

  // Method to search for a song by title and artist
  def searchSong(title: String, artist: String): Option[SongMetadata] = {
    songs.find(song => song.title.equalsIgnoreCase(title) && song.artist.equalsIgnoreCase(artist))
  }

  implicit val ec: ExecutionContext = context.system.executionContext
  implicit val materializer: Materializer = Materializer(context.system)

  val songs = importSongsFromJson("default_playlist.json")
  println(s"Imported songs: $songs")
  // Create the classic ActorSystem from the typed ActorSystem
  // Create the Listener actor
  val listener: ActorRef[SongHasFinished] = context.spawn(Listener(), "listener")

 val songsPlayer = new SongPlayer()


   // Create a Source that emits elapsed time messages for a song and a final message when the song finishes
   def createSongSource(song: SongMetadata): Source[String, _] = {
     val tickSource = Source.tick(0.seconds, 5.seconds, 5)
       .scan(0)((counter, step) => counter + step) // Increment the counter
       .takeWhile(_ <= song.duration) // Stop the source when the counter reaches 4 minutes
       .map(seconds => s"Playing song $song.title from artists ${song.artist} elapsed ${formatTime(seconds)}") // Format the message

     val endOfSongMessage = Source.single {
       listener ! SongHasFinished(song.title) // Send a message to the Listener actor
       s"Finished playing song $song.title"
     }

     tickSource.concat(endOfSongMessage) // Concatenate the end-of-song message source
   }

    override def onMessage(msg: RocolaCommand): Behavior[RocolaCommand] = {
      msg match {
        case Play() =>
          songsPlayer.playAllSongs()
          this
        case EnqueueSong(title:String,artist:String, replyTo: ActorRef[EnqueueSongResponse]) =>
          // Example search
          val searchedSong = searchSong(title,artist)
          searchedSong match {
            case Some(song) =>
              println(s"Found song: $song")
              songsPlayer.addSong(song)
              replyTo ! EnqueueSongResponse(success = true)
            case None =>
              println("Song not found")
              replyTo ! EnqueueSongResponse(success = false)
          }
          this
      }
    }
  }
