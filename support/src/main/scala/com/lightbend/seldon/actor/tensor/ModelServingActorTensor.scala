package com.lightbend.seldon.actor.tensor

import akka.actor.{ Actor, Props }
import akka.event.Logging
import com.lightbend.seldon.executors.tensor.TFBaseExecutor
import com.lightbend.seldon.stats._
import tensorflow.modelserving.avro._

/**
 * This actor is scoring every record using internal TensorFlow Model.
 * It also stores execution statistics and allows to access it.
 */

class ModelServingActorTensor(label: String, executor: TFBaseExecutor) extends Actor {

  // Log
  val log = Logging(context.system, this)
  log.info(s"Creating ModelServingActor for $label with executor ${executor.getClass.getName}")

  // Current execution state
  protected val currentExecutionState = new ModelToServeStats(executor.modelName, "Seldon model server", executor.startTime)

  // Receive message
  override def receive: PartialFunction[Any, Unit] = {

    case record: SourceRequest ⇒ // Serve data
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
object ModelServingActorTensor {

  def props(label: String, executor: TFBaseExecutor): Props = Props(new ModelServingActorTensor(label, executor))
}

