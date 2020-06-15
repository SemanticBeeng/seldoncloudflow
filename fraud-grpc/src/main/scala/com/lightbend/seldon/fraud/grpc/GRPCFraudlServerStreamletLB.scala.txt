package com.lightbend.seldon.fraud.grpc

import cloudflow.akkastream._
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._
import com.lightbend.seldon.executors.tensor.lb._
import com.lightbend.seldon.streamlet.tensor._
import tensorflow.modelserving.avro._

class GRPCFraudlServerStreamletLB extends AkkaServerStreamlet {

  // the model's parameters.
  private val modelName = "fraud"
  private val signature = ""
  private val deployment = "fraud-grpc-tfserving"

  // Streamlet
  val in = AvroInlet[SourceRequest]("card-records")
  val out = AvroOutlet[ServingResult]("ml-results")

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  println(s"Fraud Load balanced GRPPC model server. GRPC endpoint : $GRPC_TARGET")
  val executor = new SeldonTFGRPCExecutorBalancedTensor(deployment, modelName, modelName, signature, GRPC_TARGET)

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogicTensor(this, executor, in, out)
}
