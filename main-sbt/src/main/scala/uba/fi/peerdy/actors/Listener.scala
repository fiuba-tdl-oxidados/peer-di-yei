package uba.fi.peerdy.actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import uba.fi.peerdy.actors.rocola.Rocola.WrappedHttpResponse
import spray.json._
import uba.fi.peerdy.actors.rocola.Rocola

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class SongMetadata(title: String, artist: String, genre: String, duration: Int, votes:Int = 0)
// Define the JSON format for SongMetadata

case class PlaylistResponseMessage (message: String, success: Boolean, playlist: List[SongMetadata])
trait JsonSupport extends DefaultJsonProtocol {
  implicit val songMetadataFormat: RootJsonFormat[SongMetadata] = jsonFormat5(SongMetadata.apply)
  implicit val playlistResponseFormat: RootJsonFormat[PlaylistResponseMessage] = jsonFormat3(PlaylistResponseMessage.apply)
}



object Listener extends JsonSupport {
  sealed trait ListenerCommand

  final case class ProposeSong(song: String, artist: String) extends ListenerCommand
  final case class VoteSong(song: String, vote: Boolean) extends ListenerCommand


  def apply(): Behavior[ListenerCommand] = Behaviors.receive { (context, message) =>
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContextExecutor = context.executionContext
    implicit val directory:String = "localhost"
    implicit val port:String = "4545"
    val http = Http(system)

    message match {
      case ProposeSong(song, artist) =>
        val rocola = context.spawn(Rocola(), "rocola")
        rocola ! Rocola.Play()
        context.log.info(s"Proposing song: $song")
        val uri = Uri(s"http://$directory:$port/enqueue?title=$song&artist=$artist&votes=1")
        val request = HttpRequest(method = HttpMethods.GET, uri = uri)
        val responseFuture: Future[HttpResponse] = http.singleRequest(request)

        responseFuture.onComplete {
          case Success(response) =>
            Unmarshal(response.entity).to[String].onComplete {
              case Success(value) =>
                val response = value.parseJson.convertTo[PlaylistResponseMessage]
                println(s"Response: $response")
                system.terminate()
              case Failure(error) =>
                println(s"Failed to unmarshal response: $error")
                system.terminate()
            }
          case Failure(exception) =>
            println(s"Request failed: $exception")
            system.terminate()
        }
        Behaviors.same
      case VoteSong(song, vote) =>
        context.log.info(s"${if (vote) "Upvoting" else "Downvoting"} song: $song")
        //diyei ! (if (vote) DiYei.UpVoteSong(song) else DiYei.DownVoteSong(song))
        Behaviors.same
    }
  }
}
