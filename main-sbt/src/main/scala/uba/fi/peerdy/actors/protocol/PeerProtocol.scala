package uba.fi.peerdy.actors.protocol

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContextExecutor, Promise}

object PeerProtocol {

  sealed trait Command
  final case class Bind(address: String, port: Int) extends Command
  final case class Connect(address: String, port: Int, promise: Promise[Unit]) extends Command
  final case class SendMessage(message: String) extends Command
  private final case class Connected(queue: SourceQueueWithComplete[ByteString]) extends Command
  private final case class HandleServerResponse(response: String) extends Command
  private final case class HandleClientResponse(response: String) extends Command


  def apply(): Behavior[Command] = Behaviors.setup { context =>
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContextExecutor = context.executionContext
    implicit val materializer: Materializer = Materializer(context)
    val tcp = Tcp(system)

    var connections: List[SourceQueueWithComplete[ByteString]] = List.empty

    Behaviors.receiveMessage {
      // Server
      case Bind(address, port) =>
        val connections = tcp.bind(address, port)
        connections.runForeach { connection =>
          val handler = Flow[ByteString]
            .map { bs =>
              val receivedMessage = bs.utf8String.trim
              context.self ! HandleClientResponse(receivedMessage)

              // Echo the received message back to the client
              val response = ByteString(s"ACK")
              response
            }
            .alsoTo(Sink.onComplete(_ => println("Connection closed.")))

          connection.handleWith(handler)
        }
        Behaviors.same
      case HandleClientResponse(response) =>
        val args = response.split("-")
        val cmd = args(0)
        cmd match {
          case "NL" =>
            context.self ! Connect(args(1), args(2).toInt, Promise[Unit]())
          case _ =>
            println(response)
        }
        Behaviors.same

      // Client
      case Connect(address, port, promise) =>
        val outgoingConnection = tcp.outgoingConnection(address, port)

        val (queue, source) = Source.queue[ByteString](10, OverflowStrategy.dropNew)
        .preMaterialize()

        val connectionFuture = source
          .via(outgoingConnection)
          .to(Sink.foreach { response =>
            context.self ! HandleServerResponse(response.utf8String)
          })
          .run()(materializer)

        context.self ! Connected(queue)
        promise.success(())
        Behaviors.same

      case Connected(queue) =>
        connections = connections :+ queue
        Behaviors.same

      case HandleServerResponse(response) =>
        Behaviors.same

      case SendMessage(message) =>
        connections.foreach { queue =>
          queue.offer(ByteString(message))
        }
        Behaviors.same
    }
  }
}