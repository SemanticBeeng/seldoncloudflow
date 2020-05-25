package com.lightbend.seldon.executors.tensor

import java.io.File
import java.nio.file.Files

import com.google.protobuf.Descriptors
import com.lightbend.seldon.converters.TensorConverter._
import org.tensorflow._
import org.tensorflow.framework._
import tensorflow.modelserving.avro._

import scala.collection.JavaConverters._
import scala.collection.mutable.{ Map ⇒ MMap }

/**
 * Class encapsulating TensorFlow (SavedModelBundle) model processing.
 *
 * @param descriptor model descriptor
 */
class TensorFlowModelExecutorTensor(descriptor: ModelDescriptor, directory: String)
  extends TFBaseExecutor(descriptor.modelName, "model descriptor") {

  // get tags. We assume here that the first tag is the one we use
  private val tags: Seq[String] = getTags(directory)
  private val bundle = SavedModelBundle.load(directory, tags(0))
  private val graph = bundle.graph
  // get metatagraph and signature
  private val metaGraphDef = MetaGraphDef.parseFrom(bundle.metaGraphDef)
  private val signatureMap = metaGraphDef.getSignatureDefMap.asScala
  // parse signature, so that we can use definitions (if necessary) programmatically in score method
  private val signatures = parseSignatures(signatureMap)
  // get signature to use
  private val signature = descriptor.model match {
    case Some(model) ⇒ signatures.get(model.signature) match {
      case Some(s) ⇒ s
      case _       ⇒ signatures.head._2
    }
    case _ ⇒ signatures.head._2
  }
  // inputs and outputs
  private val inputNames = signature.inputs.map(inp ⇒ (inp._1 -> inp._2.name))
  private val inputNamesKeys = inputNames.keys.toSeq
  private val outputNames = signature.outputs.map(out ⇒ (out._2.name -> out._1))
  private val outputNamesKeys = outputNames.keys.toSeq

  // Create TensorFlow session
  private val session = bundle.session

  /**
   * Usage of TensorFlow bundled model for Wine scoring.
   */
  override def invokeModel(record: SourceRequest): Either[String, ServingOutput] = {
    try {
      // Create record tensors
      val requestTensors = avroToTensor(record.inputRecords.inputs)
      // we can also validate tensors types and shapes here. For now we are only testing names.
      val modelInputs = requestTensors.filter(entry ⇒ inputNamesKeys.contains(entry._1))
      // Serve model using TensorFlow APIs
      val runner = session.runner
      modelInputs.foreach { entry ⇒
        val name = inputNames.get(entry._1).get
        runner.feed(name, entry._2)
      }
      outputNames.keys.foreach(out ⇒ runner.fetch(out))
      val result = runner.run().asScala
      val mapResult = MMap[String, Tensor[_]]()
      0 to result.size - 1 foreach { i ⇒
        outputNames.get(outputNamesKeys(i)) match {
          case Some(name) ⇒ mapResult += (name -> result(i))
          case _          ⇒
        }
      }
      Right(ServingOutput(tensorToAvro(mapResult.toMap)))
    } catch {
      case t: Throwable ⇒ Left(t.getMessage)
    }
  }

  /**
   * Cleanup when a model is not used anymore
   */
  override def cleanup(): Unit = {
    try {
      session.close
    } catch {
      case t: Throwable ⇒
        println(s"WARNING: in TensorFlowBundleModel.cleanup(), call to session.close threw $t. Ignoring")
    }
    try {
      graph.close
    } catch {
      case t: Throwable ⇒
        println(s"WARNING: in TensorFlowBundleModel.cleanup(), call to graph.close threw $t. Ignoring")
    }
  }

  /**
   * Parse signatures
   *
   * @param signatures - signatures from metagraph
   * @returns map of names/signatures
   */
  private def parseSignatures(signatures: MMap[String, SignatureDef]): Map[String, Signature] = {
    signatures.map(signature ⇒
      signature._1 -> Signature(parseInputOutput(signature._2.getInputsMap.asScala), parseInputOutput(signature._2.getOutputsMap.asScala))).toMap
  }

  /**
   * Parse Input/Output
   *
   * @param inputOutputs - Input/Output definition from metagraph
   * @returns map of names/fields
   */
  private def parseInputOutput(inputOutputs: MMap[String, TensorInfo]): Map[String, Field] =
    inputOutputs.map {
      case (key, info) ⇒
        var name = ""
        var dtype: Descriptors.EnumValueDescriptor = null
        var shape = Seq.empty[Int]
        info.getAllFields.asScala.foreach { descriptor ⇒
          if (descriptor._1.getName.contains("shape")) {
            descriptor._2.asInstanceOf[TensorShapeProto].getDimList.toArray.map(d ⇒
              d.asInstanceOf[TensorShapeProto.Dim].getSize).toSeq.foreach(v ⇒ shape = shape :+ v.toInt)

          }
          if (descriptor._1.getName.contains("name")) {
            name = descriptor._2.toString.split(":")(0)
          }
          if (descriptor._1.getName.contains("dtype")) {
            dtype = descriptor._2.asInstanceOf[Descriptors.EnumValueDescriptor]
          }
        }
        key -> Field(name, dtype, shape)
    }.toMap

  /**
   * Gets all tags in the saved bundle and uses the first one. If you need a specific tag, overwrite this method
   * With a seq (of one) tags returning desired tag.
   *
   * @param directory - directory for saved model
   * @returns sequence of tags
   */
  private def getTags(directory: String): Seq[String] = {

    val d = new File(directory)
    val pbfiles = if (d.exists && d.isDirectory)
      d.listFiles.filter(_.isFile).filter(name ⇒ (name.getName.endsWith("pb") || name.getName.endsWith("pbtxt"))).toList
    else
      List[File]()
    if (pbfiles.length > 0) {
      val byteArray = Files.readAllBytes(pbfiles(0).toPath)
      SavedModel.parseFrom(byteArray).getMetaGraphsList.asScala.
        flatMap(graph ⇒ graph.getMetaInfoDef.getTagsList.asByteStringList.asScala.map(_.toStringUtf8))
    } else
      Seq.empty
  }
}

/** Definition of the field (input/output) */
case class Field(name: String, `type`: Descriptors.EnumValueDescriptor, shape: Seq[Int])

/** Definition of the signature */
case class Signature(inputs: Map[String, Field], outputs: Map[String, Field])
