package com.lightbend.seldon.tensor

import com.lightbend.seldon.executors.tensor.TensorFlowModelExecutorTensor
import com.lightbend.seldon.utils.FileUtils
import tensorflow.modelserving.avro._
import tensorflow.support.avro._
import org.scalatest._
import org.scalatest.wordspec.AsyncWordSpec

class TensorFlowModelExecutorTensorTest extends AsyncWordSpec {

  val descriptor = ModelDescriptor(
    modelName = "Recommendor model",
    description = "Recommender tensorflow saved model",
    modelSourceLocation = None,
    bucket = None
  )

  val localDirectory = FileUtils.getModelPath("data/fraud/model/1") //"/Users/boris/Projects/TFGRPC/data/recommender/model/1"

  val products = Seq(1L, 2L, 3L, 4L)
  val user = 10L
  val dtype = DataType.DT_FLOAT
  val shape = TensorShape(Seq(Dim(products.size.toLong, ""), Dim(1L, "")))
  val pTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_.toFloat)))
  val uTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_ ⇒ user.toFloat)))
  val rTensor = Tensor(dtype = dtype, tensorshape = shape)

  "Processing of model" should {
    "complete successfully" in {

      val executor = new TensorFlowModelExecutorTensor(descriptor, localDirectory)
      println("Executor created")
      val result = executor.score(SourceRequest(
        inputRecords = SourceRecord(Map("users" -> uTensor, "products" -> pTensor)),
        modelResults = ServingOutput(Map("predictions" -> rTensor))))
      println(result)
      succeed
    }
  }
}
