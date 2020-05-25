package com.lightbend.seldon.streamlet.fraud

import cloudflow.akkastream.AkkaStreamlet
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import tensorflow.modelserving.avro.ServingResult

final case object NormalResultConsoleEgressTensor extends AkkaStreamlet {

  // Input
  val in = AvroInlet[ServingResult]("inference-result")
  // Shape
  final override val shape = StreamletShape.withInlets(in)

  // Create logic
  override def createLogic = ConsoleEgressLogic(in, "Normal transaction")
}
