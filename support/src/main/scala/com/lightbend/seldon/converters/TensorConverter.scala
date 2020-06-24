package com.lightbend.seldon.converters

import tensorflow.support.avro._

import scala.collection.JavaConverters._
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.protobuf.ByteString
import org.tensorflow.framework.TensorShapeProto
import org.tensorflow.framework.TensorShapeProto._
import org.tensorflow.framework.TensorProto
import tensorflow.serving._
import java.lang.reflect.Type
import java.nio._
import java.util

object TensorConverter {

  private val gson = new Gson

  def avroToJSON(signature_name: String, tensors: Map[String, Tensor]): String = {
    val dataMap = tensors.map {
      case (n, t) ⇒
        val dimensions = t.tensorshape.dim.map(d ⇒ d.size.toInt).reverse.dropRight(1)
        var data: Array[Any] = t.dtype match {
          case DataType.DT_FLOAT  ⇒ t.float_data.get.toArray
          case DataType.DT_DOUBLE ⇒ t.double_data.get.toArray
          case DataType.DT_INT64  ⇒ t.long_data.get.toArray
          case DataType.DT_INT32  ⇒ t.int_data.get.toArray
          case DataType.DT_BOOL   ⇒ t.boolean_data.get.toArray
          case _                  ⇒ t.string_data.get.toArray
        }
        dimensions.foreach { d ⇒
          data = data.sliding(d).toArray
        }
        (n, data)
    }.asJava
    /*
    dataMap.size() match {
      case size if size == 1 ⇒ """{"signature_name":"""" + signature_name + """",""" + gson.toJson(dataMap).substring(1)
      case _                 ⇒ gson.toJson(Request(signature_name, dataMap))
    }
*/
    gson.toJson(Request(signature_name, dataMap))
  }

  def JSONToAvro(tensors: Map[String, Tensor], message: String): Map[String, Tensor] = {
    val result: java.util.Map[String, Array[Any]] = tensors.size match {
      case size if size == 1 ⇒
        val typeMap: Type = new TypeToken[java.util.Map[String, Array[Any]]] {}.getType()
        gson.fromJson(message, typeMap)
      case _ ⇒
        gson.fromJson(message, classOf[Response]).outputs
    }
    result.asScala.map {
      case (name, value) ⇒
        val flatten = flaten(value.toSeq)
        var nm = name
        val tensor = tensors.size match {
          case 1 ⇒
            nm = tensors.keys.head
            Some(tensors.values.head)
          case _ ⇒ tensors.get(name)
        }
        val rTensor = tensor match {
          case Some(t) ⇒
            val newTensor = t.dtype match {
              case DataType.DT_FLOAT  ⇒ Tensor(dtype = t.dtype, tensorshape = t.tensorshape, float_data = Some(flatten.map(_.toFloat)))
              case DataType.DT_DOUBLE ⇒ Tensor(dtype = t.dtype, tensorshape = t.tensorshape, double_data = Some(flatten.map(_.toDouble)))
              case DataType.DT_INT64  ⇒ Tensor(dtype = t.dtype, tensorshape = t.tensorshape, long_data = Some(flatten.map(_.toLong)))
              case DataType.DT_INT32  ⇒ Tensor(dtype = t.dtype, tensorshape = t.tensorshape, int_data = Some(flatten.map(_.toInt)))
              case DataType.DT_BOOL   ⇒ Tensor(dtype = t.dtype, tensorshape = t.tensorshape, boolean_data = Some(flatten.map(_.toBoolean)))
              case _                  ⇒ Tensor(dtype = t.dtype, tensorshape = t.tensorshape, string_data = Some(flatten))
            }
            newTensor
          case _ ⇒ Tensor(DataType.DT_STRING, TensorShape(Seq.empty))
        }
        (nm, rTensor)
    }.toMap
  }

  private def flaten(original: Seq[Any]): Seq[String] = {
    if (original(0).isInstanceOf[util.ArrayList[_]])
      original.toSeq.map { value ⇒
        flaten(value.asInstanceOf[util.ArrayList[Any]].asScala)
      }.flatten
    else
      original.map(_.toString)
  }

  def avroToProto(modelname: String, signaturename: String, tensors: Map[String, Tensor]): PredictRequest = {
    val dataMap = tensors.map {
      case (n, t) ⇒
        val tensorshape = Some(TensorShapeProto(t.tensorshape.dim.map(dim ⇒ Dim(dim.size))))
        val tensorProto = t.dtype match {
          case DataType.DT_FLOAT  ⇒ TensorProto(dtype = org.tensorflow.framework.DataType.DT_FLOAT, tensorShape = tensorshape, floatVal = t.float_data.get)
          case DataType.DT_DOUBLE ⇒ TensorProto(dtype = org.tensorflow.framework.DataType.DT_DOUBLE, tensorShape = tensorshape, doubleVal = t.double_data.get)
          case DataType.DT_INT64  ⇒ TensorProto(dtype = org.tensorflow.framework.DataType.DT_INT64, tensorShape = tensorshape, int64Val = t.long_data.get)
          case DataType.DT_INT32  ⇒ TensorProto(dtype = org.tensorflow.framework.DataType.DT_INT32, tensorShape = tensorshape, intVal = t.int_data.get)
          case DataType.DT_BOOL   ⇒ TensorProto(dtype = org.tensorflow.framework.DataType.DT_BOOL, tensorShape = tensorshape, boolVal = t.boolean_data.get)
          case _                  ⇒ TensorProto(dtype = org.tensorflow.framework.DataType.DT_STRING, tensorShape = tensorshape, stringVal = t.string_data.get.map(s ⇒ ByteString.copyFrom(s.getBytes)))
        }
        (n, tensorProto)
    }
    val model = ModelSpec(name = modelname, signatureName = signaturename)
    PredictRequest(modelSpec = Some(model), inputs = dataMap)
  }

  def protoToAvro(response: PredictResponse): Map[String, Tensor] = {
    response.outputs.map {
      case (name, t) ⇒
        val tensorshape = TensorShape(t.tensorShape.get.dim.map(dim ⇒ Dim(dim.size, "")))
        val tensor = t.dtype match {
          case org.tensorflow.framework.DataType.DT_FLOAT ⇒
            Tensor(dtype = DataType.DT_FLOAT, tensorshape = tensorshape, float_data = Some(t.floatVal))
          case org.tensorflow.framework.DataType.DT_DOUBLE ⇒
            Tensor(dtype = DataType.DT_DOUBLE, tensorshape = tensorshape, double_data = Some(t.doubleVal))
          case org.tensorflow.framework.DataType.DT_INT64 ⇒
            Tensor(dtype = DataType.DT_INT64, tensorshape = tensorshape, long_data = Some(t.int64Val))
          case org.tensorflow.framework.DataType.DT_INT32 ⇒
            Tensor(dtype = DataType.DT_INT32, tensorshape = tensorshape, int_data = Some(t.intVal))
          case org.tensorflow.framework.DataType.DT_BOOL ⇒
            Tensor(dtype = DataType.DT_BOOL, tensorshape = tensorshape, boolean_data = Some(t.boolVal))
          case _ ⇒
            Tensor(dtype = DataType.DT_STRING, tensorshape = tensorshape, string_data = Some(t.stringVal.map(_.toString)))
        }
        (name, tensor)
    }
  }

  def avroToTensor(tensors: Map[String, Tensor]): Map[String, org.tensorflow.Tensor[_]] = {
    tensors.map {
      case (n, t) ⇒
        val shape = t.tensorshape.dim.map(_.size).toArray
        val tensor = t.dtype match {
          case DataType.DT_FLOAT ⇒
            val data = t.float_data.get
            val buffer = FloatBuffer.allocate(data.size)
            data.foreach(buffer.put(_))
            buffer.flip()
            org.tensorflow.Tensor.create(shape, buffer)
          case DataType.DT_DOUBLE ⇒
            val data = t.double_data.get
            val buffer = DoubleBuffer.allocate(data.size)
            data.foreach(buffer.put(_))
            buffer.flip()
            org.tensorflow.Tensor.create(shape, buffer)
          case DataType.DT_INT64 ⇒
            val data = t.long_data.get
            val buffer = LongBuffer.allocate(data.size)
            data.foreach(buffer.put(_))
            buffer.flip()
            org.tensorflow.Tensor.create(shape, buffer)
          case DataType.DT_INT32 ⇒
            val data = t.int_data.get
            val buffer = IntBuffer.allocate(data.size)
            data.foreach(buffer.put(_))
            buffer.flip()
            org.tensorflow.Tensor.create(shape, buffer)
          case DataType.DT_BOOL ⇒
            val data = t.boolean_data.get
            val buffer = ByteBuffer.allocate(data.size)
            data.foreach { v ⇒ buffer.put(if (v) 1.toByte else 0.toByte) }
            buffer.flip()
            org.tensorflow.Tensor.create(classOf[java.lang.Boolean], shape, buffer)
          case _ ⇒
            val data = t.string_data.get
            val bsize = data.map(_.size).fold(0)(_ + _) + 4 * data.size
            val buffer = ByteBuffer.allocate(bsize)
            data.foreach { s ⇒
              buffer.putInt(s.size)
              buffer.put(s.getBytes())
            }
            buffer.flip()
            org.tensorflow.Tensor.create(classOf[java.lang.String], shape, buffer)
        }
        (n, tensor)
    }.toMap
  }

  def tensorToAvro(result: Map[String, org.tensorflow.Tensor[_]]): Map[String, Tensor] = {
    result.map {
      case (n, t) ⇒
        val tensorshape = TensorShape(t.shape().toSeq.map(Dim(_, "")))
        val tensor = t.dataType() match {
          case org.tensorflow.DataType.FLOAT ⇒
            val buffer = FloatBuffer.allocate(t.numElements())
            t.writeTo(buffer)
            Tensor(dtype = DataType.DT_FLOAT, tensorshape = tensorshape, float_data = Some(buffer.array()))
          case org.tensorflow.DataType.DOUBLE ⇒
            val buffer = DoubleBuffer.allocate(t.numElements())
            t.writeTo(buffer)
            Tensor(dtype = DataType.DT_DOUBLE, tensorshape = tensorshape, double_data = Some(buffer.array()))
          case org.tensorflow.DataType.INT64 ⇒
            val buffer = LongBuffer.allocate(t.numElements())
            t.writeTo(buffer)
            Tensor(dtype = DataType.DT_INT64, tensorshape = tensorshape, long_data = Some(buffer.array()))
          case org.tensorflow.DataType.INT32 ⇒
            val buffer = IntBuffer.allocate(t.numElements())
            t.writeTo(buffer)
            Tensor(dtype = DataType.DT_INT32, tensorshape = tensorshape, int_data = Some(buffer.array()))
          case org.tensorflow.DataType.BOOL ⇒
            val buffer = ByteBuffer.allocate(t.numElements())
            t.writeTo(buffer)
            Tensor(dtype = DataType.DT_BOOL, tensorshape = tensorshape, boolean_data = Some(buffer.array().map(_ != 0)))
          case _ ⇒
            val buffer = ByteBuffer.allocate(t.numBytes())
            t.writeTo(buffer)
            buffer.flip()
            val array = new Array[String](t.numElements)
            0 to array.size - 1 foreach { i ⇒
              val length = buffer.getInt
              val stringBuffer = new Array[Byte](length)
              buffer.get(stringBuffer)
              array(i) = new String(stringBuffer)
            }
            Tensor(dtype = DataType.DT_STRING, tensorshape = tensorshape, string_data = Some(array))
        }
        (n, tensor)
    }
  }
}

case class Request(signature_name: String, inputs: java.util.Map[String, Array[Any]])

case class Response(outputs: java.util.Map[String, Array[Any]])
