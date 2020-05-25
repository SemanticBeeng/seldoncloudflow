package com.lightbend.seldon.executors

import pipelines.examples.modelserving.recommender.avro._

abstract class SeldonBaseExecutor(name: String, source: String) extends Serializable {

  val startTime = System.currentTimeMillis()
  val modelName = name

  /**
   * Actual scoring.
   * @param record - Record to serve
   * @return Either error or invocation result.
   */
  def invokeModel(record: RecommenderRecord): Either[String, RecommenderServingOutput]

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
  def score(record: RecommenderRecord): RecommenderResult = {
    val start = System.currentTimeMillis()
    val (errors, modelOutput) = invokeModel(record) match {
      case Left(errors)  ⇒ (errors, RecommenderServingOutput(Seq.empty, Seq.empty))
      case Right(output) ⇒ ("", output)
    }
    val duration = (System.currentTimeMillis() - start)
    val resultMetadata = ModelResultMetadata(errors, modelName, source, startTime, duration)
    RecommenderResult(record, modelOutput, resultMetadata)
  }
}
