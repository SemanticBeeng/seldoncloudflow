package com.lightbend.seldon.stats

import akka.actor.ActorRef
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import scala.concurrent.duration._

// Route for the statistics access
object QueriesAkkaHttpResource extends JacksonSupport {

  implicit val askTimeout = Timeout(30.seconds)

  def storeRoutes(modelserver: ActorRef): Route =
    get {
      // Get statistics for a given data type
      path("state") {
        onSuccess(modelserver.ask(GetState()).mapTo[ModelToServeStats]) {
          case stats: ModelToServeStats ⇒
            complete(ConvertedModelToServeStats(stats))
        }
      }
    }
}
