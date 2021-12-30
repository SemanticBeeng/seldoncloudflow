package com.lightbend.seldon

import com.google.gson.Gson
import com.lightbend.seldon.executors._
import pipelines.examples.modelserving.recommender.avro._
import org.scalatest._

import org.scalatest.wordspec.AsyncWordSpec

class SeldonTFRESTExecutorTest extends AsyncWordSpec {

  // the model's name.
  val modelName = "recommender"
  val path = "http://localhost:8003/seldon/seldon/rest-tfserving/v1/models/recommender/:predict"

  val products = Seq(1L, 2L, 3L, 4L)

  val input = new RecommenderRecord(10L, products)

  "Conversion of data" should {
    "complete successfully" in {

      val gson = new Gson

      val products = input.products.map(p ⇒ Array(p.toDouble)).toArray
      val users = input.products.map(_ ⇒ Array(input.user.toDouble)).toArray
      val request = Request("", RequestInputs(products, users))
      val str = gson.toJson(request)
      println(s"request created $str")
      succeed
    }
  }

  "Processing of model" should {
    "complete successfully" in {

      val executor = new SeldonTFRESTExecutor(modelName, path)
      println("Model created")
      val result = executor.score(input)
      println(result)
      succeed
    }
  }
}
