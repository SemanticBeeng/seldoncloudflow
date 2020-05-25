package com.lightbend.seldon

import org.scalatest.FlatSpec
import pipelines.examples.modelserving.recommender.avro._
import com.lightbend.seldon.executors._

class SeldonTFGRPCExecutorTest extends FlatSpec {

  val host = "localhost"
  val port = 8003

  // the model's name.
  val modelName = "recommender"

  val products = Seq(1L, 2L, 3L, 4L)

  val input = new RecommenderRecord(10L, products)

  "Processing of model" should "complete successfully" in {

    val executor = new SeldonTFGRPCExecutor(modelName, host, port)
    println("Model created")
    val result = executor.score(input)
    println(result)
  }
}