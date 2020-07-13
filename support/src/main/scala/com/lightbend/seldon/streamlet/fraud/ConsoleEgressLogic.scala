package com.lightbend.seldon.streamlet.fraud

import cloudflow.akkastream.AkkaStreamletContext
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.CodecInlet
import tensorflow.modelserving.avro._
import tensorflow.support.avro._

/**
 * Abstraction for writing to output to the console (i.e., stdout).
 *
 * @param inlet CodecInlet for records of type ServingResult
 */
case class ConsoleEgressLogic(
    inlet: CodecInlet[ServingResult], message: String)
  (implicit context: AkkaStreamletContext)
  extends RunnableGraphStreamletLogic {

  // Write model serving results
  private def write(record: ServingResult): Unit = {
    println(message)
    // Inputs
    println("inputs:")
    record.inputRecord.inputs.foreach {
      case (name, tensor) ⇒ write(name, tensor)
    }

    // Results
    println("results:")
    record.modelResult.outputs.foreach {
      case (name, tensor) ⇒ write(name, tensor)
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

  // Runnable graph
  def runnableGraph = sourceWithCommittableContext(inlet).map(write(_)).to(committableSink)
}
