package com.lightbend.seldon.executors.tensor

import tensorflow.modelserving.avro._

abstract class TFBaseExecutor(name: String, source: String) extends Serializable {

  val startTime = System.currentTimeMillis()
  val modelName = name

  /**
   * Actual scoring.
   * @param record - Record to serve
   * @return Either error or invocation result.
   */
  def invokeModel(record: SourceRequest): Either[String, ServingOutput]

  /**
   * Cleanup when a model is not used anymore
   */
  def cleanup(): Unit

  /**
   * Score a record with the model
   *
   * @param record - Record to serve
   * @return RecommenderResult, including the result, scoring metadata (possibly including an error string), and some scoring metadata.
   */
  def score(record: SourceRequest): ServingResult = {
    val start = System.currentTimeMillis()
    val (errors, modelOutput) = invokeModel(record) match {
      case Left(errors)  ⇒ (errors, ServingOutput(Map.empty))
      case Right(output) ⇒ ("", output)
    }
    val duration = (System.currentTimeMillis() - start)
    val resultMetadata = ModelResultMetadata(errors, modelName, source, startTime, duration)
    ServingResult(record.inputRecords, modelOutput, resultMetadata)
  }
}
