package uba.fi.verysealed

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import uba.fi.verysealed.rocola.RocolaManager
import uba.fi.verysealed.rocola.routes.EnqueueSongRouteHandler

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object DirectoryHttpServer  {


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[RocolaManager.RocolaCommand] = ActorSystem(RocolaManager(), "my-system")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val enqueueRouteHandler = new EnqueueSongRouteHandler(system,system)(10.seconds)
    //TODO: add additional routes here
    //val anotherRouteHandler = new AnotherRouteHandler()

    val route: Route = concat(
      enqueueRouteHandler.route
    )

    val bindingFuture = Http().newServerAt("localhost", 4545).bind(route)

    println(s"Server now online. Please navigate to http://localhost:4545/enqueue?title=songTitle&artist=songArtist\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}



