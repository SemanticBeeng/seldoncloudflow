package com.lightbend.seldon.utils

import java.io._

import scala.io.Source

object DataThinner {

  val original = "data/fraud/data/creditcard.csv"
  val thinned = "data/fraud/data/small-creditcard.csv"

  def main(args: Array[String]): Unit = {

    val bufferedSource = Source.fromFile(original)
    val out = new BufferedWriter(new FileWriter(thinned))
    var firstLine = true
    var linenumber = 0
    var nline = 0
    bufferedSource.getLines.foreach { line ⇒
      firstLine match {
        case true ⇒ firstLine = false // Skip first line
        case _ ⇒
          val record = line.split(",")
          record.last match {
            case cl if cl.equals("\"1\"") ⇒
              out.write(line, 0, line.size)
              out.write("\n")
              nline = nline + 1
            case _ ⇒
              linenumber = linenumber + 1
              if (linenumber >= 20) {
                linenumber = 0
                out.write(line, 0, line.size)
                out.write("\n")
                nline = nline + 1
              }
          }
      }
    }
    println(s"wrote $nline records")
    bufferedSource.close
    out.flush()
    out.close()
    val bSource = Source.fromFile(original)
    nline = 0
    bSource.getLines.foreach { line ⇒
      if (nline < 5) println(line)
      nline = nline + 1
    }
    bSource.close()
  }
}
