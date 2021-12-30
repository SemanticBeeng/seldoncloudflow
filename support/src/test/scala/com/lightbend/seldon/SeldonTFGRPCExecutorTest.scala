package com.lightbend.seldon

import pipelines.examples.modelserving.recommender.avro._
import com.lightbend.seldon.executors._
import org.scalatest._

import org.scalatest.wordspec.AsyncWordSpec

class SeldonTFGRPCExecutorTest extends AsyncWordSpec {

  val host = "localhost"
  val port = 8003

  // the model's name.
  val modelName = "recommender"

  val products = Seq(1L, 2L, 3L, 4L)

  val input = new RecommenderRecord(10L, products)

  "Processing of model" should {
    "complete successfully" in {

      val executor = new SeldonTFGRPCExecutor(modelName, host, port)
      println("Model created")
      val result = executor.score(input)
      println(result)
      succeed
    }
  }
}
