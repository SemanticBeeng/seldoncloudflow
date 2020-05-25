package com.lightbend.seldon.fraud

import com.lightbend.seldon.streamlet.fraud.FraudRecordGeneratorTensor
import org.scalatest.FlatSpec

class FraudTensorDataGeneratorTest extends FlatSpec {

  "Generation of data" should "complete successfully" in {
    0 to 5 foreach { _ ⇒
      println(FraudRecordGeneratorTensor.generateRecord())
    }
  }
}