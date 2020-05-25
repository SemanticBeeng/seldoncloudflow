package com.lightbend.seldon.utils

import java.io._
import java.util.zip.{ ZipEntry, ZipInputStream, ZipOutputStream }

/**
 * Support class encapsulating zipping operations. Used for passing zipped TF model
 */
object Zipper {
  /**
   * Recursively delete files in directory
   * @param directory - directory (File)
   */
  private def deleteRecursively(directory: File): Unit = {
    if (directory.exists) {
      if (directory.isDirectory) {
        directory.listFiles.foreach(deleteRecursively)
      }
      val _ = directory.delete
    }
  }

  /**
   * Delete files in directory
   * @param directory - directory (File)
   */
  private def deleteDirectoryContent(directory: File): Unit = {
    if (directory.exists) {
      if (directory.isDirectory)
        directory.listFiles.foreach(deleteRecursively)
    }
  }

  /**
   * Unzip message into directory
   * @param data - message (byte array)
   * @param directory - directory (File)
   */
  def unzipMessage(data: Array[Byte], directory: String): String = {
    val destination = new File(directory)
    deleteDirectoryContent(destination)
    val zis = new ZipInputStream(new ByteArrayInputStream(data))
    Stream.continually(zis.getNextEntry).takeWhile(_ != null).filter(!_.isDirectory).foreach(entry ⇒ {
      //      println(s"Unzipping file ${entry.getName}")
      val outPath = destination.toPath.resolve(entry.getName)
      val outPathParent = outPath.getParent
      if (!outPathParent.toFile.exists()) {
        outPathParent.toFile.mkdirs()
      }
      val outFile = outPath.toFile
      val out = new FileOutputStream(outFile)
      val buffer = new Array[Byte](4096)
      Stream.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(out.write(buffer, 0, _))
      out.close()
    })
    destination.listFiles(_.isDirectory).head.getAbsolutePath
  }

  /**
   * Recursively zip content of a directory
   * @param zos - zip output stream
   * @param fileToZip - file to zip
   * @param parentDirectoryName - parent directory
   */
  def addDirToZipArchive(zos: ZipOutputStream, fileToZip: File, parentDirectoryName: Option[String] = None): Unit =
    if (fileToZip != null || fileToZip.exists) {
      val zipEntryName = parentDirectoryName match {
        case Some(name) ⇒ s"$name/${fileToZip.getName}"
        case _          ⇒ fileToZip.getName
      }
      fileToZip.isDirectory match {
        case true ⇒ // Process directory
          fileToZip.listFiles.foreach(addDirToZipArchive(zos, _, Some(zipEntryName)))
        case _ ⇒ //individual file
          zos.putNextEntry(new ZipEntry(zipEntryName))
          val fis = new FileInputStream(fileToZip)
          val buffer = new Array[Byte](4096)
          Stream.continually(fis.read(buffer)).takeWhile(_ != -1).foreach(zos.write(buffer, 0, _))
          zos.closeEntry()
          fis.close()
      }
    }

  /**
   * Zip directory into message
   * @param source - directory (File)
   * @return message
   */
  def getZippedMessage(source: File): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val zos = new ZipOutputStream(bos)
    addDirToZipArchive(zos, source, None)
    bos.close()
    bos.toByteArray
  }
}
