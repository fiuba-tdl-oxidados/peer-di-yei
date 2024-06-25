package uba.fi.verysealed

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{BroadcastHub, Keep}
import uba.fi.verysealed.rocola.{RocolaManager, SessionManager}
import uba.fi.verysealed.rocola.behavior.PlaybackStatusStream
import uba.fi.verysealed.rocola.routes.{PlaybackRouteHandler, PlaylistRouteHandler, RegisterRouteHandler}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object DirectoryHttpServer  {


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[RocolaManager.RocolaCommand] = ActorSystem(RocolaManager(), "my-system")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    // Get the status source for routes
    PlaybackStatusStream.initialize()(system)
    val statusSource = PlaybackStatusStream.getSource

    val sessionManager: ActorRef[SessionManager.Command] = system.systemActorOf(SessionManager(), "sessionManager")

    // Set up routes
    val enqueueRouteHandler = new PlaylistRouteHandler(system,sessionManager,system)(10.seconds,executionContext)
    val  playbackRouteHandler= PlaybackRouteHandler(statusSource,system,sessionManager)
    // Set up SessionManager actor
    val registerRoutes = RegisterRouteHandler(sessionManager)
    val route: Route = concat(
      enqueueRouteHandler.route,
      playbackRouteHandler,
      registerRoutes
    )

    val bindingFuture = Http().newServerAt("localhost", 4545).bind(route)

    println(s"Server now online. Please navigate to http://localhost:4545/enqueue?title=songTitle&artist=songArtist\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}



