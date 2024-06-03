package uba.fi.peerdy.actors.rocola.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import uba.fi.peerdy.actors.rocola.Rocola.{PlaySessionCommand, PlaySessionEvent, PublishPlaySessionMessage}

object PlayingBehavior {
  def apply(
            room: ActorRef[PublishPlaySessionMessage],
            clientName: String,
            client: ActorRef[PlaySessionEvent]
           ): Behavior[PlaySessionCommand] = {

    Behaviors.setup(ctx => new PlayingBehavior(ctx, room, clientName, client))
  }
}

private class PlayingBehavior (context: ActorContext[PlaySessionCommand],
                       room: ActorRef[PublishPlaySessionMessage],
                       clientName: String,
                       client: ActorRef[PlaySessionEvent]
                      ) extends AbstractBehavior[PlaySessionCommand](context) {

  override def onMessage(msg: PlaySessionCommand): Behavior[PlaySessionCommand] = {
    msg match {
      case _ => this
    }
  }
}