package com.lightbend.seldon.tensor

import com.lightbend.seldon.executors.tensor._
import org.scalatest.FlatSpec
import tensorflow.modelserving.avro._
import tensorflow.support.avro._

// To run this test, execute the following command:
// kubectl port-forward $(kubectl get pods -n seldon -l app.kubernetes.io/name=ambassador -o jsonpath='{.items[0].metadata.name}') -n seldon 8003:8080
class SeldonTFRESTExecutorTensorTest extends FlatSpec {

  // the model's name.
  val signature = ""
  val modelName = "recommender"
  val path = "http://localhost:8003/seldon/seldon/rest-tfserving/v1/models/recommender/:predict"

  val products = Seq(1L, 2L, 3L, 4L)
  val user = 10L
  val dtype = DataType.DT_FLOAT
  val shape = TensorShape(Seq(Dim(products.size.toLong, ""), Dim(1L, "")))
  val pTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_.toFloat)))
  val uTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_ => user.toFloat)))
  val rTensor = Tensor(dtype = dtype, tensorshape = shape)

  "Processing of model" should "complete successfully" in {

    val executor = new SeldonTFRESTExecutorTensor(modelName, signature, path)
    println("Model created")
    val result = executor.score(SourceRequest(inputRecords = SourceRecord(Map("users" -> uTensor, "products" -> pTensor)),
      modelResults = ServingOutput(Map("predictions" -> rTensor))))
    println(result)
  }
}