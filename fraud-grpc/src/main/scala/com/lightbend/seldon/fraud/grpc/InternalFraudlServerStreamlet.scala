package com.lightbend.seldon.fraud.grpc

import cloudflow.akkastream._
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._
import com.lightbend.seldon.executors.tensor._
import com.lightbend.seldon.streamlet.tensor._
import com.lightbend.seldon.utils.FileUtils
import tensorflow.modelserving.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration.{ GRPC_FRAUD_MODEL_PATH }

class InternalFraudlServerStreamlet extends AkkaServerStreamlet {

  // the model's parameters.
  val descriptor = ModelDescriptor(
    modelName = "Fraud model",
    description = "Fraud tensorflow saved model",
    modelSourceLocation = None,
    bucket = None
  )

  val localDirectory = GRPC_FRAUD_MODEL_PATH

  // Streamlet
  val in = AvroInlet[SourceRequest]("card-records")
  val out = AvroOutlet[ServingResult]("ml-results")

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  lazy val executor = {
    println(s"Fraud model server with local model at : $localDirectory")
    new TensorFlowModelExecutorTensor(descriptor, localDirectory)
  }

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogicTensor(this, executor, in, out)
}
