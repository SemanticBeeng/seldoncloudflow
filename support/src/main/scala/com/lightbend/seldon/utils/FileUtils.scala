package com.lightbend.seldon.utils

import java.io.File

object FileUtils {
  val projectRootDirectory = "seldoncloudflow"

  def getModelPath(path: String) = {
    val projectRoot = new File(".").getCanonicalPath
    s"${projectRoot.split(projectRootDirectory)(0)}$projectRootDirectory/$path"
  }

}
