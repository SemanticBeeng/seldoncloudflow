package com.lightbend.seldon

import com.lightbend.seldon.configuration.ModelServingConfiguration.{ GRPC_HOST, GRPC_PORT }
import com.lightbend.seldon.executors._
import pipelines.examples.modelserving.recommender.avro._

import org.scalatest._
import org.scalatest.wordspec.AsyncWordSpec

class SeldonTFGRPCExecutorTest extends AsyncWordSpec {

  /*  val host = "10.0.141.249"
  val port = 8500*/

  // the model's name.
  val modelName = "recommender"

  val products = Seq(1L, 2L, 3L, 4L)

  val input = new RecommenderRecord(10L, products)

  "Processing of model" should {
    "complete successfully" in {

      val executor = new SeldonTFGRPCExecutor(modelName, GRPC_HOST, GRPC_PORT)
      println("Model created")
      val result = executor.score(input)
      println(result)
      succeed
    }
  }
}
