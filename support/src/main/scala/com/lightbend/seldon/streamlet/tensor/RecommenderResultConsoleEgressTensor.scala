package com.lightbend.seldon.streamlet.tensor

import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import tensorflow.modelserving.avro.ServingResult
import tensorflow.support.avro.{ DataType, Tensor }

final case object RecommenderResultConsoleEgressTensor extends AkkaStreamlet {

  // Input
  val in = AvroInlet[ServingResult]("inference-result")
  // Shape
  final override val shape = StreamletShape.withInlets(in)

  // Create logic
  override def createLogic = new RunnableGraphStreamletLogic() {

    // Runnable graph
    def runnableGraph = sourceWithCommittableContext(in).map(write(_)).to(committableSink)
  }

  // Write model serving results
  private def write(record: ServingResult): Unit = {
    // Inputs
    println("inputs:")
    record.inputRecord.inputs.foreach {
      case (name, tensor) ⇒
        write(name, tensor)
    }
    // Results
    println("results:")
    record.modelResult.outputs.foreach {
      case (name, tensor) ⇒
        write(name, tensor)
    }
    // Metadata
    println(s"execution metadata: ${record.modelResultMetadata.toString}")
  }

  // write tensor name pairs
  private def write(name: String, t: Tensor): Unit = {
    val data = t.dtype match {
      case DataType.DT_FLOAT  ⇒ t.float_data.get.map(_.toString)
      case DataType.DT_DOUBLE ⇒ t.double_data.get.map(_.toString)
      case DataType.DT_INT64  ⇒ t.long_data.get.map(_.toString)
      case DataType.DT_INT32  ⇒ t.int_data.get.map(_.toString)
      case DataType.DT_BOOL   ⇒ t.boolean_data.get.map(_.toString)
      case _                  ⇒ t.string_data.get
    }
    println(s"$name -> $data")
  }
}
