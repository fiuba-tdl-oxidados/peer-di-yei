package uba.fi.peerdy.actors.protocol

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import akka.actor.typed.ActorSystem
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

object DiYeiProtocol {

  sealed trait Command
  final case class Bind(address: String, port: Int) extends Command
  final case class ConnectionAccepted(connection: Tcp.IncomingConnection) extends Command
  final case class SendMessage(connection: ActorRef[ByteString], message: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContextExecutor = context.executionContext
    implicit val materializer: Materializer = Materializer(context)
    val tcp = Tcp(system)

    Behaviors.receiveMessage{
      case Bind(address, port) =>
        val connections = tcp.bind(address, port)
        connections.runForeach { connection =>
          val handler = Flow[ByteString]
            .map { bs =>
              val receivedMessage = bs.utf8String.trim
              println(s"Received: $receivedMessage")

              // Echo the received message back to the client
              val response = ByteString(s"Echo: $receivedMessage\n")
              response
            }
            .alsoTo(Sink.onComplete(_ => println("Connection closed.")))

          connection.handleWith(handler)
        }
        Behaviors.same
    }
  }
}