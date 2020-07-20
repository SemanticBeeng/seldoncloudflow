package com.lightbend.seldon.fraud.grpc

import cloudflow.akkastream._
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import com.lightbend.seldon.executors.tensor._
import com.lightbend.seldon.streamlet.tensor._
import tensorflow.modelserving.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._

class GRPCFraudlServerStreamlet extends AkkaServerStreamlet {

  // the model's parameters.
  private val modelName = "fraud"
  private val signature = ""
  private val deployment = "fraud-grpc-tfserving"

  // Streamlet
  val in = AvroInlet[SourceRequest]("card-records")
  val out = AvroOutlet[ServingResult]("ml-results")

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  println(s"Fraud model server. GRPC endpoint : $GRPC_HOST:$GRPC_PORT")
  val executor = new SeldonTFGRPCExecutorTensor(deployment, modelName, modelName, signature, GRPC_HOST, GRPC_PORT)

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogicTensor(this, executor, in, out)
}
