package com.lightbend.seldon.executors

import com.google.gson._
import pipelines.examples.modelserving.recommender.avro._
import scalaj.http._

class SeldonTFRESTExecutor(modelName: String, source: String) extends SeldonBaseExecutor(modelName, source) {

  private val gson = new Gson

  override def invokeModel(record: RecommenderRecord): Either[String, RecommenderServingOutput] = {

    // Build request json
    val products = record.products.map(p ⇒ Array(p.toDouble)).toArray
    val users = record.products.map(_ ⇒ Array(record.user.toDouble)).toArray
    val request = Request("", RequestInputs(products, users))
    val requestString = gson.toJson(request)

    try {
      val response = Http(source).postData(requestString).header("content-type", "application/json").asString
      response.code match {
        case code if code == 200 ⇒ // Got successful response
          val prediction = gson.fromJson(response.body, classOf[RecommendationOutputs])
          val predictions = prediction.outputs.map(_(0))
            .zip(record.products).map(r ⇒ (r._2.toString, r._1)).unzip
          Right(RecommenderServingOutput(predictions._1, predictions._2))
        case _ ⇒ // Got error response
          Left(response.body)
      }
    } catch {
      case t: Throwable ⇒
        println(s"Error accessing HTTP server $source")
        t.printStackTrace()
        Left(t.getMessage)
    }
  }

  /**
   * Cleanup when a model is not used anymore. Nothing to do in this case
   */
  override def cleanup(): Unit = {}
}

case class RequestInputs(products: Array[Array[Double]], users: Array[Array[Double]])
case class Request(signature_name: String, inputs: RequestInputs)
case class RecommendationOutputs(outputs: Array[Array[Double]])

