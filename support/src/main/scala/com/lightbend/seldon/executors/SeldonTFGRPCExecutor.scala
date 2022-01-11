package com.lightbend.seldon.executors

import io.grpc._
import io.grpc.stub._
import org.tensorflow.framework.tensor._
import org.tensorflow.framework.tensor_shape._
import org.tensorflow.framework.types._
import pipelines.examples.modelserving.recommender.avro._
import tensorflow.serving.model._
import tensorflow.serving.predict._
import tensorflow.serving.prediction_service._

class SeldonTFGRPCExecutor(modelName: String, host: String, port: Int) extends SeldonBaseExecutor(modelName, s"$host:$port") {

  import SeldonTFGRPCExecutor._

  // Headers
  val headers = new Metadata()
  headers.put(SELDON_KEY, "grpc-tfserving")
  headers.put(NAMESPACE_KEY, "seldon")

  // create a stub
  val channelbuilder = ManagedChannelBuilder.forAddress(host, port)
  channelbuilder.usePlaintext()
  val channel = channelbuilder.build()
  val server = PredictionServiceGrpc.blockingStub(channel)
  val serverWithHeaders = MetadataUtils.attachHeaders(server, headers)

  // Model spec
  val model = ModelSpec(name = modelName, signatureName = "serving_default")

  /**
   * Actual scoring.
   *
   * @param record - Record to serve
   * @return Either error or invocation result.
   */
  override def invokeModel(record: RecommenderRecord): Either[String, RecommenderServingOutput] = {
    // Build products and users proto
    val tensorshape = Some(TensorShapeProto(Seq(TensorShapeProto.Dim(record.products.size.toLong), TensorShapeProto.Dim(1l))))
    val productProto = TensorProto(dtype = DataType.DT_FLOAT, tensorShape = tensorshape, floatVal = record.products.map(_.toFloat))
    val userProto = TensorProto(dtype = DataType.DT_FLOAT, tensorShape = tensorshape, floatVal = record.products.map(_ ⇒ record.user.toFloat))

    // Create request
    val request = PredictRequest(modelSpec = Some(model), inputs = Map("products" -> productProto, "users" -> userProto))
    try {
      val response = serverWithHeaders.predict(request)
      println(s"Request ${request.toString}")
      println(s"Responce ${response.toString}")
      val probabilities = response.outputs.get("predictions").get.floatVal
      val predictions = probabilities.map(_.toDouble)
        .zip(record.products).map(r ⇒ (r._2.toString, r._1)).unzip
      Right(RecommenderServingOutput(predictions._1, predictions._2))
    } catch {
      case t: Throwable ⇒ Left(t.getMessage)
    }
  }

  /**
   * Cleanup when a model is not used anymore
   */
  override def cleanup(): Unit = {
    val _ = channel.shutdown()
  }
}

object SeldonTFGRPCExecutor {
  val SELDON_KEY = Metadata.Key.of("seldon", Metadata.ASCII_STRING_MARSHALLER)
  val NAMESPACE_KEY = Metadata.Key.of("namespace", Metadata.ASCII_STRING_MARSHALLER)
}
