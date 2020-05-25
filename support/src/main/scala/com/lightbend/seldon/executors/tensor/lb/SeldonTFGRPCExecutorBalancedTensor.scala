package com.lightbend.seldon.executors.tensor.lb

import com.lightbend.seldon.converters.TensorConverter._
import com.lightbend.seldon.executors.SeldonTFGRPCExecutor
import com.lightbend.seldon.executors.tensor.TFBaseExecutor
import io.grpc._
import io.grpc.stub._
//import io.grpc.util.RoundRobinLoadBalancerFactory
import tensorflow.modelserving.avro._
import tensorflow.serving.prediction_service._

// Implementation is based on
// https://github.com/saturnism/grpc-by-example-java/blob/master/kubernetes-lb-example/echo-client-lb-api/src/main/java/com/example/grpc/client/ClientSideLoadBalancedEchoClient.java
// Note:
// The highest GRPC version that supports this is 1.20.0. For the higher version code has to changed
//
// A target here is the URl used by the loadbalancer: kubernetes:///{namespace}/{service}/{port}
// where namespace/service is the name/location of the service and port is the port used by pod - not service,
// for example: "kubernetes:///seldon/ambassador/8080"

class SeldonTFGRPCExecutorBalancedTensor(deployment: String, modelName: String, model: String, signature: String, target: String)
  extends TFBaseExecutor(modelName, target) {

  import SeldonTFGRPCExecutor._

  // Headers
  val headers = new Metadata()
  headers.put(SELDON_KEY, deployment)
  headers.put(NAMESPACE_KEY, "seldon")

  // create a channel
  val channelbuilder = ManagedChannelBuilder.forTarget(target)
  channelbuilder.nameResolverFactory(new KubernetesNameResolverProvider())
  //  channelbuilder.loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
  channelbuilder.usePlaintext()
  val channel = channelbuilder.build()

  // create a stub
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

