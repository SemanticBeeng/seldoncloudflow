package com.lightbend.seldon.utils

import java.io._

import io.minio.MinioClient
import com.lightbend.seldon.configuration.ModelServingConfiguration._

import scala.collection.JavaConverters._

object MinioUtils {

  println(s"Using Minio: url - $MINIO_URL, key - $MINIO_KEY, secret - $MINIO_SECRET")
  // Create a minioClient with the MinIO Server URL, Access key and Secret key.
  val client = new MinioClient(MINIO_URL, MINIO_KEY, MINIO_SECRET)

  // Get list of flies starting with prefix
  def getFilesPrefix(bucket: String, prefix: String): Seq[String] = {
    client.listObjects(bucket, prefix).asScala.map(_.get().objectName()).toSeq
  }

  // Get object content
  def getObjectContent(bucket: String, name: String): Array[Byte] = {
    var stream: InputStream = null
    try {
      stream = client.getObject(bucket, name)
      Stream.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray
    } catch {
      case t: Throwable ⇒
        println(s"Error reading file $name from bucket $bucket - ${t.getMessage}")
        Array[Byte]()
    } finally {
      if (stream != null)
        stream.close
    }
  }

  // Get object to the file
  def getObjectToFile(bucket: String, name: String, file: String): Unit = {
    try {
      client.getObject(bucket, name, file)
    } catch {
      case t: Throwable ⇒
        println(s"Error writing file $name from bucket $bucket- ${t.getMessage}")
    }
  }

  // Write object
  def writeObjectContent(bucket: String, name: String, content: Array[Byte]): Unit = {
    val bis = new ByteArrayInputStream(content)
    try {
      client.putObject(bucket, name, bis, null, null, null, "application/octet-stream")
    } catch {
      case t: Throwable ⇒
        println(s"Error writing file $name from bucket $bucket- ${t.getMessage}")
    }
  }

  // remove current directory file
  def deleteFile(bucket: String, name: String): Unit = {
    try {
      client.removeObject(bucket, name)
    } catch {
      case t: Throwable ⇒
        println(s"Error deleting file $name from bucket $bucket- ${t.getMessage}")
    }
  }

  def copyModelLocally(location: String, bucket: String, files: Seq[String]): Unit = {
    val commonpath = commonPath(files)
    files.foreach(f ⇒ {
      val destination = s"$location/${f.substring(commonpath.length, f.length)}"
      val file = new File(destination)
      // make sure file and its parent exist.
      val parent = file.getParentFile
      if (!parent.exists()) parent.mkdir()
      if (!file.exists()) file.createNewFile()
      getObjectToFile(bucket, f, destination)
      println(s"loaded new file $destination from MINIO")
    })
  }

  def commonPath(paths: Seq[String]): String = {
      def common(a: List[String], b: List[String]): List[String] = (a, b) match {
        case (a :: as, b :: bs) if a equals b ⇒ a :: common(as, bs)
        case _                                ⇒ Nil
      }
    if (paths.length < 2) paths.headOption.getOrElse("")
    else paths.map(_.split("/").toList).reduceLeft(common).mkString("/") + "/"
  }
}
