package uba.fi.peerdy.actors.protocol

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

object PeerProtocol {

  sealed trait Command
  final case class Bind(address: String, port: Int) extends Command
  final case class Connect(address: String, port: Int) extends Command
  final case class SendMessage(message: String) extends Command
  private final case class Connected(queue: SourceQueueWithComplete[ByteString]) extends Command
  private final case class HandleResponse(cause: String) extends Command


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
              println(s"Received: $receivedMessage")

              val args = receivedMessage.split("-")
              val cmd = args(0)
              cmd match {
                case "NL" =>
                  println(args)
                  // context.self ! Connect(args(1), args(2).toInt)
              }

              // Echo the received message back to the client
              val response = ByteString(s"Echo: $receivedMessage\n")
              response
            }
            .alsoTo(Sink.onComplete(_ => println("Connection closed.")))

          connection.handleWith(handler)
        }
        Behaviors.same

      // Client
      case Connect(address, port) =>
        val outgoingConnection = tcp.outgoingConnection(address, port)

        val (queue, source) = Source.queue[ByteString](10, OverflowStrategy.dropNew)
        .preMaterialize()

        val connectionFuture = source
          .via(outgoingConnection)
          .to(Sink.foreach { response =>
            context.self ! HandleResponse(response.utf8String)
          })
          .run()(materializer)

        context.self ! Connected(queue)
        Behaviors.same

      case Connected(queue) =>
        connections = connections :+ queue
        println("Connected to DiYeiProtocol server")
        Behaviors.same

      case HandleResponse(response) =>
        context.log.info("Received response from server: {}", response)
        Behaviors.same

      case SendMessage(message) =>
        connections.foreach { queue =>
          queue.offer(ByteString(message)).onComplete {
              case Success(result) => println("Message sent: {}", result)
              case Failure(exception) => context.log.error("Failed to send message: {}", exception)
            }
        }
        Behaviors.same
    }
  }
}