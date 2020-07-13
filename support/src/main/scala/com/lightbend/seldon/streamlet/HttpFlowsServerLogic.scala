package com.lightbend.seldon.streamlet

import akka._
import akka.actor._
import akka.http.scaladsl._
import akka.kafka._
import akka.pattern.ask
import akka.stream.scaladsl._
import akka.util.Timeout
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import com.lightbend.seldon.actor.ModelServingActor
import com.lightbend.seldon.executors._
import com.lightbend.seldon.stats.QueriesAkkaHttpResource
import pipelines.examples.modelserving.recommender.avro._

import scala.concurrent.duration._
import scala.util.Failure

class HttpFlowsServerLogic(
    server:   Server,
    executor: SeldonBaseExecutor,
    inlet:    AvroInlet[RecommenderRecord],
    outlet:   AvroOutlet[RecommenderResult])
  (implicit context: AkkaStreamletContext) extends ServerStreamletLogic(server) {

  implicit val askTimeout: Timeout = Timeout(30.seconds)

  // Model serving actor
  val modelserver: ActorRef = system.actorOf(ModelServingActor.props("recommender", executor))

  override def run(): Unit = {

    sourceWithCommittableContext(inlet).via(dataFlow).runWith(committableSink(outlet))
    startServer(context, containerPort)
  }

  // Data processing
  def dataFlow: FlowWithContext[RecommenderRecord, ConsumerMessage.Committable, RecommenderResult, ConsumerMessage.Committable, NotUsed] =
    FlowWithCommittableContext[RecommenderRecord]
      .mapAsync(1) { record ⇒ modelserver.ask(record).mapTo[RecommenderResult] }

  def startServer(
      context: AkkaStreamletContext,
      port:    Int): Unit = {
    val routes = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, "0.0.0.0", port)
      .map { binding ⇒
        context.signalReady()
        system.log.info(s"Bound to ${binding.localAddress.getHostName}:${binding.localAddress.getPort}")
        // this only completes when StreamletRef executes cleanup.
        context.onStop { () ⇒
          system.log.info(s"Unbinding from ${binding.localAddress.getHostName}:${binding.localAddress.getPort}")
          binding.unbind().map(_ ⇒ Dun)
        }
        binding
      }
      .andThen {
        case Failure(cause) ⇒
          system.log.error(cause, s"Failed to bind to $port.")
          context.stop()
      }
    ()
  }
}

