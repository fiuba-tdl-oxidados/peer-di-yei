package uba.fi.peerdy.actors.rocola.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import uba.fi.peerdy.actors.rocola.Rocola.{PlayMessagePosted, NotifyDiYei, PlayMessagePosted, PlaySessionCommand, PlaySessionDenied, PlaySessionStarted, PublishPlaySessionMessage, StartPlaySession}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AuthenticationBehavior (context: ActorContext[PlaySessionCommand]) extends AbstractBehavior[PlaySessionCommand](context) {
    private var currentSession: Option[ActorRef[PlaySessionCommand]] = None

    override def onMessage(message: PlaySessionCommand): Behavior[PlaySessionCommand] = {
      message match {

        case StartPlaySession(clientName, client) =>
          if (currentSession.isEmpty) {
            val ses = context.spawn(
              PlayingBehavior(context.self, clientName, client),
              name = URLEncoder.encode(clientName, StandardCharsets.UTF_8.name))
            client ! PlaySessionStarted(currentSession.get)
            currentSession = Some(ses)
          } else {
            val reason = "Another session is already in progress. Only one session is allowed at a time."
            client ! PlaySessionDenied(reason)
          }
          this
        case PublishPlaySessionMessage(clientName, message) =>
          if (currentSession.isEmpty) {
            val reason = "No session is currently active. Please start a session first."
            context.log.warn(reason)
          } else {
            currentSession.get ! NotifyDiYei(PlayMessagePosted(clientName, message))
          }
          this
      }
    }


}
