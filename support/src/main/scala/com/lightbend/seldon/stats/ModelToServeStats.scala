package com.lightbend.seldon.stats

import java.time.Instant

/**
 * Model serving statistics definition
 */
case class ModelToServeStats(
    name:         String = "",
    description:  String = "",
    since:        Long   = 0,
    var usage:    Long   = 0,
    var duration: Double = .0,
    var min:      Long   = Long.MaxValue,
    var max:      Long   = Long.MinValue) {

  /**
   * Increment model serving statistics; invoked after scoring every record.
   * @arg execution Long value for the milliseconds it took to score the record.
   */
  def incrementUsage(execution: Long): ModelToServeStats = {
    usage = usage + 1
    duration = duration + execution
    if (execution < min) min = execution
    if (execution > max) max = execution
    this
  }
}

/**
 * Converted model serving statistics for HTTP
 */
case class ConvertedModelToServeStats(
    name:        String = "",
    description: String = "",
    since:       String = "",
    usage:       Long   = 0,
    average:     Double = .0,
    min:         Long   = Long.MaxValue,
    max:         Long   = Long.MinValue
)

object ConvertedModelToServeStats {
  def apply(stats: ModelToServeStats): ConvertedModelToServeStats = {
    stats.usage match {
      case occured if (occured == 0) ⇒ // No data
        ConvertedModelToServeStats()
      case _ ⇒ // there is data
        ConvertedModelToServeStats(stats.name, stats.description, Instant.ofEpochMilli(stats.since).toString,
          stats.usage, stats.duration / stats.usage, stats.min, stats.max)
    }
  }
}

/**
 * Get model serving statistics command
 */
case class GetState()
