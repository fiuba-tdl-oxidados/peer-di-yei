/* 
package uba.fi.peerdy.actors.protocol

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, ActorContext}
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, Materializer, SystemMaterializer}
import akka.util.ByteString
import scala.concurrent.ExecutionContextExecutor

object PeerProtocol {
  sealed trait Command
  case class AddConnection(address: String, port: Int) extends Command
  case class RemoveConnection(connection: ActorRef[Connection.Command]) extends Command
  case class SendMessageToAll(message: String) extends Command
  case class ReceivedMessage(connection: ActorRef[Connection.Command], message: String) extends Command
  case class StartServer(interface: String, port: Int) extends Command
  case class ClientConnected(connection: ActorRef[Connection.Command]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    implicit val system = context.system
    implicit val executionContext = context.executionContext
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    val tcp = Tcp(system)

    var connections: Set[ActorRef[Connection.Command]] = Set.empty

    Behaviors.receiveMessage {
      case AddConnection(address, port) =>
        val connection = context.spawn(Connection(address, port, context.self), s"connection-$address-$port")
        connections += connection
        Behaviors.same

      case RemoveConnection(connection) =>
        connections -= connection
        Behaviors.same

      case SendMessageToAll(message) =>
        connections.foreach(_ ! Connection.WriteMessage(message))
        Behaviors.same

      case ReceivedMessage(connection, message) =>
        context.log.info(s"Received message from $connection: $message")
        Behaviors.same

      case StartServer(host, port) =>
        val binding = tcp.bind(host, port)
        binding.to(Sink.foreach { connection =>
          val handler = context.spawn(ConnectionHandler(context.self), s"handler-${connection.remoteAddress}")
          handler ! ConnectionHandler.HandleConnection(connection)
        }).run()
        context.log.info(s"Server started at $host:$port")
        Behaviors.same

      case ClientConnected(connection) =>
        connections += connection
        Behaviors.same
    }
  }
}

object Connection {
  sealed trait Command
  case class WriteMessage(message: String) extends Command
  private case class WrappedReceivedMessage(message: String) extends Command

  def apply(address: String, port: Int, replyTo: ActorRef[PeerProtocol.Command]): Behavior[Command] = Behaviors.setup { context =>
    implicit val system = context.system
    implicit val executionContext = context.executionContext
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    val tcp = Tcp(system)

    val connectionFlow = tcp.outgoingConnection(address, port)
    val (writeQueue, source): (SourceQueueWithComplete[ByteString], Source[ByteString, NotUsed]) = 
      Source.queue[ByteString](bufferSize = 10).preMaterialize()
    val sink = Flow[ByteString]
      .map(bytes => WrappedReceivedMessage(bytes.utf8String))
      .to(Sink.actorRef(context.self, onCompleteMessage = Behaviors.stopped))

    source.via(connectionFlow).to(sink).run()

    Behaviors.receiveMessage {
      case WriteMessage(message) =>
        writeQueue.offer(ByteString(message))
        Behaviors.same

      case WrappedReceivedMessage(message) =>
        replyTo ! PeerProtocol.ReceivedMessage(context.self, message)
        Behaviors.same
    }
  }
}

object ConnectionHandler {
  sealed trait Command
  case class HandleConnection(connection: Tcp.IncomingConnection) extends Command

  def apply(peerProtocol: ActorRef[PeerProtocol.Command]): Behavior[Command] = Behaviors.setup { context =>
    implicit val system = context.system
    implicit val executionContext = context.executionContext
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    val tcp = Tcp(system)

    Behaviors.receiveMessage {
      case HandleConnection(connection) =>
        val handler = context.spawn(ConnectionActor(connection, peerProtocol), s"connection-${connection.remoteAddress}")
        connection.handleWith(Flow[ByteString]
          .map { bytes =>
            handler ! ConnectionActor.IncomingMessage(bytes.utf8String)
            bytes
          }
          .mapMaterializedValue(_ => handler)
        )
        Behaviors.same
    }
  }
}

object ConnectionActor {
  sealed trait Command
  case class IncomingMessage(message: String) extends Command

  def apply(connection: Tcp.IncomingConnection, peerProtocol: ActorRef[PeerProtocol.Command]): Behavior[Command] = Behaviors.setup { context =>
    implicit val system = context.system
    implicit val executionContext = context.executionContext
    implicit val materializer: Materializer = SystemMaterializer(system).materializer
    val tcp = Tcp(system)
    
    peerProtocol ! PeerProtocol.ClientConnected(context.self)

    Behaviors.receiveMessage {
      case IncomingMessage(message) =>
        peerProtocol ! PeerProtocol.ReceivedMessage(context.self, message)
        Behaviors.same
    }
  }
}
  */