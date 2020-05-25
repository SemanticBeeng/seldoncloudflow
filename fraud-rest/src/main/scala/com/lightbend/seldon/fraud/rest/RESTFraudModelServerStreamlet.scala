package com.lightbend.seldon.fraud.rest

import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import com.lightbend.seldon.executors.tensor._
import com.lightbend.seldon.streamlet.tensor._
import tensorflow.modelserving.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._

class RESTFraudModelServerStreamlet extends AkkaServerStreamlet {

  // the model's parameters.
  private val modelName = "fraud"
  private val signature = ""

  // Streamlet
  val in = AvroInlet[SourceRequest]("card-records")
  val out = AvroOutlet[ServingResult]("ml-results")

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  println(s"Fraud model server. Rest endpoint : $REST_PATH")
  val executor = new SeldonTFRESTExecutorTensor(modelName, signature, REST_PATH)

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogicTensor(this, executor, in, out)
}
