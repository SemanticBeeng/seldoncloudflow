package com.lightbend.seldon.rest

import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import com.lightbend.seldon.executors._
import com.lightbend.seldon.streamlet.HttpFlowsServerLogic
import pipelines.examples.modelserving.recommender.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._

class RESTModelServerStreamlet extends AkkaServerStreamlet {

  val in = AvroInlet[RecommenderRecord]("recommender-records")
  val out = AvroOutlet[RecommenderResult]("recommender-results", _.inputRecord.datatype)

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  lazy val executor = {
    println(s"Starting Rest Model Serving with location $REST_PATH")
    new SeldonTFRESTExecutor("recommender", REST_PATH)
  }

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogic(this, executor, in, out)
}
