package com.lightbend.seldon.streamlet.fraud

import akka.NotUsed
import akka.stream.scaladsl._
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import tensorflow.modelserving.avro._
import tensorflow.support.avro._
import com.lightbend.seldon.configuration.ModelServingConfiguration._

import scala.concurrent.duration._

/**
 * Ingress of data for recommendations. In this case, every @dataFrequencyMilliseconds we
 * load and send downstream one record that is randomly generated.
 */
final case object FraudRecordIngressTensor extends AkkaStreamlet {

  // Output
  val out = AvroOutlet[SourceRequest]("source-records")

  // Shape
  final override val shape = StreamletShape.withOutlets(out)

  // Create Logic
  override final def createLogic = new RunnableGraphStreamletLogic {

    println(s"Starting Data Ingress with frequency $DATA_FREQUENCY")
    // Runnable graph
    def runnableGraph =
      FraudRecordIngressUtilsTensor.makeSource(DATA_FREQUENCY).to(plainSink(out))
  }
}

/**
 * An object generating a stream of input records for recommendations
 */
object FraudRecordIngressUtilsTensor {
  // Data frequency
  lazy val dataFrequencyMilliseconds: FiniteDuration = 5.millisecond // 200 msg per sec

  // Make source
  def makeSource(frequency: FiniteDuration = dataFrequencyMilliseconds): Source[SourceRequest, NotUsed] = {
    Source.repeat(FraudRecordGeneratorTensor)
      .map(gen ⇒ gen.generateRecord())
      .throttle(1, frequency)
  }
}

/**
 * An object generating individual recommendation's records
 */
object FraudRecordGeneratorTensor {

  // Data type
  private val dtype = DataType.DT_FLOAT
  // Input/Output names
  private val input = "transaction"
  private val output = "predictions"
  // Data records
  val records = getListOfDataRecords(DATA_FILE)
  var recordsIterator = records.iterator

  // Generate new request record
  def generateRecord(): SourceRequest = {

    // Next record
    val data = nextRecord()
    // Shape
    val shape = TensorShape(Seq(Dim(1L, ""), Dim(data.size.toLong, "")))
    // Tensors
    val tTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(data))
    val pTensor = Tensor(dtype = dtype, tensorshape = shape)
    // Source record
    SourceRequest(
      inputRecords = SourceRecord(Map(input -> tTensor)),
      modelResults = ServingOutput(Map(output -> pTensor)))
  }

  private def nextRecord(): Seq[Float] = {
    recordsIterator.hasNext match {
      case false ⇒ recordsIterator = records.iterator
      case _     ⇒
    }
    recordsIterator.next()
  }

  private def getListOfDataRecords(file: String): Seq[Seq[Float]] = {

    var result = Seq.empty[Seq[Float]]
    //    val bufferedSource = scala.io.Source.fromFile(file)
    val is = getClass.getResourceAsStream(file)
    val bufferedSource = scala.io.Source.fromInputStream(is)
    bufferedSource.getLines.foreach { line ⇒
      val record = line.split(",").map(_.trim).drop(1).dropRight(1).map(_.toFloat).toSeq
      result = record +: result
    }
    bufferedSource.close
    result
  }
}
