package com.lightbend.seldon.actor

import akka.actor.{ Actor, Props }
import akka.event.Logging
import com.lightbend.seldon.executors._
import com.lightbend.seldon.stats._
import pipelines.examples.modelserving.recommender.avro._

/**
 * This actor is scoring every record using internal TensorFlow Model.
 * An additional stream allows to update the model
 */

class ModelServingActor(label: String, executor: SeldonBaseExecutor) extends Actor {

  // Log
  val log = Logging(context.system, this)
  log.info(s"Creating ModelServingActor for $label with executor ${executor.getClass.getName}")

  // Current execution state
  protected val currentExecutionState = new ModelToServeStats(executor.modelName, "Seldon model server", executor.startTime)

  // Recieve message
  override def receive: PartialFunction[Any, Unit] = {

    case record: RecommenderRecord ⇒ // Serve data
      val result = executor.score(record)
      currentExecutionState.incrementUsage(result.modelResultMetadata.duration)
      sender() ! result
    case _: GetState ⇒ // State query
      sender() ! currentExecutionState
    case unknown ⇒ // Unknown message
      log.error(s"ModelServingActor: Unknown actor message received: $unknown")
  }
}

// Factory
object ModelServingActor {

  def props(label: String, executor: SeldonBaseExecutor): Props = Props(new ModelServingActor(label, executor))
}
