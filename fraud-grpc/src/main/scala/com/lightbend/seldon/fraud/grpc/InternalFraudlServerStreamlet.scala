package com.lightbend.seldon.fraud.grpc

import cloudflow.akkastream._
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._
import com.lightbend.seldon.executors.tensor._
import com.lightbend.seldon.streamlet.tensor._
import tensorflow.modelserving.avro._

class InternalFraudlServerStreamlet extends AkkaServerStreamlet {

  // the model's parameters.
  val descriptor = ModelDescriptor(
    modelName = "Fraud model",
    description = "Fraud tensorflow saved model",
    modelSourceLocation = None,
    bucket = None
  )

  val localDirectory = "/Users/boris/Projects/TFGRPC/data/fraud/model/1"

  // Streamlet
  val in = AvroInlet[SourceRequest]("card-records")
  val out = AvroOutlet[ServingResult]("ml-results")

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  println(s"Fraud model server with local model at : $localDirectory")
  val executor = new TensorFlowModelExecutorTensor(descriptor, localDirectory)

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogicTensor(this, executor, in, out)
}
