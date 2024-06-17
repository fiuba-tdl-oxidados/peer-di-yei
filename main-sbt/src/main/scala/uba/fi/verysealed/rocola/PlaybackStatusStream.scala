package uba.fi.verysealed.rocola.behavior

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import akka.actor.typed.scaladsl.adapter._
import uba.fi.verysealed.rocola.behavior.playlist.PlaybackStatusChanged


object PlaybackStatusStream {
  private var queue: Option[SourceQueueWithComplete[PlaybackStatusChanged]] = None
  private var source: Option[Source[PlaybackStatusChanged, _]] = None

  def initialize()(implicit system: ActorSystem[_]): Unit = {
    implicit val materializer: Materializer = Materializer(system)

    val (newQueue, newSource) = Source.queue[PlaybackStatusChanged](bufferSize = 100, OverflowStrategy.dropHead)
      .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
      .run()

    queue = Some(newQueue)
    source = Some(newSource)
  }

  def getSource: Source[PlaybackStatusChanged, _] = {
    source.getOrElse(throw new IllegalStateException("PlaybackStatusStream not initialized"))
  }

  def getQueue: SourceQueueWithComplete[PlaybackStatusChanged] = {
    queue.getOrElse(throw new IllegalStateException("PlaybackStatusStream not initialized"))
  }
}
