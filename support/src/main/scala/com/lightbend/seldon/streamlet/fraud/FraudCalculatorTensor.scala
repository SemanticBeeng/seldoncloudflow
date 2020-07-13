package com.lightbend.seldon.streamlet.fraud

import akka.NotUsed
import akka.kafka.ConsumerMessage.Committable
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ GraphDSL, Partition, RunnableGraph }
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import tensorflow.modelserving.avro._

/**
 * Ingress of data for recommendations. In this case, every @dataFrequencyMilliseconds we
 * load and send downstream one record that is randomly generated.
 */
final case object FraudCalculatorTensor extends AkkaStreamlet {

  // Input/Output keys
  private val input = "transaction"
  private val output = "predictions"

  // threshold
  val threshold = 4.0

  // Input
  val in = AvroInlet[ServingResult]("inference-result")
  // Output
  val normals = AvroOutlet[ServingResult]("normal-transactions")
  val frauds = AvroOutlet[ServingResult]("fraud-transactions")
  val errors = AvroOutlet[ServingResult]("error-transactions")

  // Shape
  final override val shape = StreamletShape.withInlets(in).withOutlets(normals, frauds, errors)

  // Create Logic
  override final def createLogic = new RunnableGraphStreamletLogic {
    // Runnable graph
    def runnableGraph = {
      RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] ⇒
          import GraphDSL.Implicits._

          // The partitioning lambda
          val partition = builder.add(Partition[(ServingResult, Committable)](3, message ⇒ {
            val source = message._1.inputRecord.inputs.get(input)
            val result = message._1.modelResult.outputs.get(output)
            if (source.isDefined && result.isDefined) {
              var error = 0.0
              val s = source.get.float_data.get.map(_.toDouble)
              val r = result.get.float_data.get.map(_.toDouble)
              0 to s.size - 1 foreach { i ⇒
                error = error + scala.math.pow((s(i) - r(i)), 2)
              }
              error = error / s.size
              if (error < threshold) 0 else 1
            } else 2
          }))

          sourceWithCommittableContext(in) ~> partition.in

          partition.out(0) ~> committableSink(normals)
          partition.out(1) ~> committableSink(frauds)
          partition.out(2) ~> committableSink(errors)

          ClosedShape
        }
      )
    }
  }
}
