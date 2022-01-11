package com.lightbend.seldon.executors.tensor

import com.lightbend.seldon.converters.TensorConverter._
import com.lightbend.seldon.executors.SeldonTFGRPCExecutor
import io.grpc._
import io.grpc.stub._
import tensorflow.modelserving.avro._

import tensorflow.serving.prediction_service._

class SeldonTFGRPCExecutorTensor(deployment: String, modelName: String, model: String, signature: String, host: String, port: Int)
  extends TFBaseExecutor(modelName, s"$host:$port") {

  import SeldonTFGRPCExecutor._

  // Headers
  val headers = new Metadata()
  headers.put(SELDON_KEY, deployment)
  headers.put(NAMESPACE_KEY, "seldon")

  // create a stub
  val channelbuilder = ManagedChannelBuilder.forAddress(host, port)
  channelbuilder.usePlaintext()
  val channel = channelbuilder.build()
  val server = PredictionServiceGrpc.blockingStub(channel)
  val serverWithHeaders = MetadataUtils.attachHeaders(server, headers)

  /**
   * Actual scoring.
   *
   * @param record - Record to serve
   * @return Either error or invocation result.
   */
  override def invokeModel(record: SourceRequest): Either[String, ServingOutput] = {
    // Create request
    val request = avroToProto(model, signature, record.inputRecords.inputs)
    try {
      val response = serverWithHeaders.predict(request)
      Right(ServingOutput(protoToAvro(response)))
    } catch {
      case t: Throwable ⇒ {
        t.printStackTrace()
        Left(t.getMessage)
      }
    }
  }

  /**
   * Cleanup when a model is not used anymore
   */
  override def cleanup(): Unit = {
    val _ = channel.shutdown()
  }
}

