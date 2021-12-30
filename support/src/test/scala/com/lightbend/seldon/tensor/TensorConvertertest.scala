package com.lightbend.seldon.tensor

import com.lightbend.seldon.converters.TensorConverter._

import tensorflow.serving.predict.PredictResponse
import tensorflow.support.avro._
import org.scalatest._

import org.scalatest.wordspec.AsyncWordSpec

class TensorConvertertest extends AsyncWordSpec {

  private val products = Seq(1L, 2L, 3L, 4L)
  private val boolean = Seq(true, true, false, false)
  private val string = Seq("one", "two", "three", "four")
  private val user = 10L
  private val dtype = DataType.DT_FLOAT
  private val shape = TensorShape(Seq(Dim(products.size.toLong, ""), Dim(1L, "")))
  private val pTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_.toFloat)))
  private val uTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_ ⇒ user.toFloat)))
  private val shape1 = TensorShape(Seq(Dim(products.size.toLong, "")))
  private val pTensor1 = Tensor(dtype = dtype, tensorshape = shape1, float_data = Some(products.map(_.toFloat)))
  private val bTensor = Tensor(dtype = DataType.DT_BOOL, tensorshape = shape, boolean_data = Some(boolean))
  private val sTensor = Tensor(dtype = DataType.DT_STRING, tensorshape = shape, string_data = Some(string))

  private val rTensor = Tensor(dtype = dtype, tensorshape = shape)
  private val rTensor1 = Tensor(dtype = DataType.DT_STRING, tensorshape = shape)
  private val message = """{"outputs":[[0.01f],[0.02f],[0.03f],[0.04f]]}"""
  private val message1 = """{"outputs":["0.01","0.02","0.03","0.04"]}"""
  private val message2 = """{"outputs":[[["0.01"],["0.02"]],[["0.03"],["0.04"]]]}"""

  "Converting feom Avro to FSON" should {
    "work correctly" in {
      println(avroToJSON("", Map("users" -> uTensor, "products" -> pTensor)))
      println(avroToJSON("", Map("products" -> pTensor1)))
      succeed
    }
  }

  "Converting feom JSON to Avro" should {
    "work correctly" in {
      println(JSONToAvro(Map("outputs" -> rTensor), message))
      println(JSONToAvro(Map("outputs" -> rTensor1), message1))
      println(JSONToAvro(Map("outputs" -> rTensor1), message2))
      succeed
    }
  }

  "Converting feom Avro to Proto" should {
    "work correctly" in {
      val proto = avroToProto("recommender", "", Map("users" -> uTensor, "products" -> pTensor))
      println(proto)
      succeed
    }
  }

  "Converting from Proto to Avro" should {
    "work correctly" in {
      val proto = avroToProto("recommender", "", Map("users" -> uTensor, "products" -> pTensor))
      val prediction = PredictResponse(outputs = proto.inputs)
      println(protoToAvro(prediction))
      succeed
    }
  }

  "Converting feom Avro to Tensors" should {
    "work correctly" in {
      println(avroToTensor(Map("users" -> uTensor, "products" -> pTensor)))
      println(avroToTensor(Map("boolean" -> bTensor)))
      println(avroToTensor(Map("string" -> sTensor)))
      succeed
    }
  }

  "Converting from Tensors to Avro" should {
    "work correctly" in {
      val floatTensor = org.tensorflow.Tensor.create(Array(Array(1.0f, 2.0f, 3.0f), Array(4.0f, 5.0f, 6.0f)))
      val boolTensor = org.tensorflow.Tensor.create(Array(Array(true, false, true), Array(false, true, false)))
      val stringTensor = avroToTensor(Map("string" -> sTensor))
      println(tensorToAvro(Map("float" -> floatTensor)))
      println(tensorToAvro(Map("boolean" -> boolTensor)))
      println(tensorToAvro(stringTensor))
      succeed
    }
  }

  "Testing prediction error" should {
    "work correctly" in {
      val source = Seq(-1.3598071336738, -0.0727811733098497, 2.53634673796914, 1.37815522427443, -0.338320769942518, 0.462387777762292, 0.239598554061257, 0.0986979012610507, 0.363786969611213, 0.0907941719789316, -0.551599533260813, -0.617800855762348, -0.991389847235408, -0.311169353699879, 1.46817697209427, -0.470400525259478, 0.207971241929242, 0.0257905801985591, 0.403992960255733, 0.251412098239705, -0.018306777944153, 0.277837575558899, -0.110473910188767, 0.0669280749146731, 0.128539358273528, -0.189114843888824, 0.133558376740387, -0.0210530534538215, 149.62)
      val source1 = Seq(1.22965763450793, 0.141003507049326, 0.0453707735899449, 1.20261273673594, 0.191880988597645, 0.272708122899098, -0.00515900288250983, 0.0812129398830894, 0.464959994783886, -0.0992543211289237, -1.41690724314928, -0.153825826253651, -0.75106271556262, 0.16737196252175, 0.0501435942254188, -0.443586797916727, 0.00282051247234708, -0.61198733994012, -0.0455750446637976, -0.21963255278686, -0.167716265815783, -0.270709726172363, -0.154103786809305, -0.780055415004671, 0.75013693580659, -0.257236845917139, 0.0345074297438413, 0.00516776890624916, 4.99)
      val source2 = Seq(-0.529912284186556, 0.873891581460326, 1.34724732930113, 0.145456676582257, 0.414208858362661, 0.10022309405219, 0.711206082959649, 0.1760659570625, -0.286716934699997, -0.484687683196852, 0.872489590125871, 0.851635859904339, -0.571745302934562, 0.100974273045751, -1.51977183258512, -0.284375978261788, -0.310523584869201, -0.404247868800905, -0.823373523914155, -0.290347610865436, 0.0469490671140629, 0.208104855076299, -0.185548346773547, 0.00103065983293288, 0.0988157011025622, -0.552903603040518, -0.0732880835681738, 0.0233070451077205, 6.14)
      val source3 = Seq(2.06800684906669, 0.207265984679455, -1.64664564880114, 0.435716176822372, 0.422147728996579, -0.924940167851775, 0.231379534447201, -0.27959438863959, 0.389964773147564, -0.397183344200216, -0.36646132381896, 0.52119154731507, 0.686642734353729, -0.965637137761214, 0.127438329914494, 0.256346007570947, 0.474350191010161, -0.419845767569896, 0.00968890987862715, -0.11802931903998, -0.350415449423717, -0.873302202997522, 0.344752529315008, 0.656184816796339, -0.250235268276369, 0.16310725486743, -0.0583981002259504, -0.0280112887519432, 1.88)

      val source4 = Seq(-2.3122265423263, 1.95199201064158, -1.60985073229769, 3.9979055875468, -0.522187864667764, -1.42654531920595, -2.53738730624579, 1.39165724829804, -2.77008927719433, -2.77227214465915, 3.20203320709635, -2.89990738849473, -0.595221881324605, -4.28925378244217, 0.389724120274487, -1.14074717980657, -2.83005567450437, -0.0168224681808257, 0.416955705037907, 0.126910559061474, 0.517232370861764, -0.0350493686052974, -0.465211076182388, 0.320198198514526, 0.0445191674731724, 0.177839798284401, 0.261145002567677, -0.143275874698919, 0)
      val source5 = Seq(-2.30334956758553, 1.759247460267, -0.359744743330052, 2.33024305053917, -0.821628328375422, -0.0757875706194599, 0.562319782266954, -0.399146578487216, -0.238253367661746, -1.52541162656194, 2.03291215755072, -6.56012429505962, 0.0229373234890961, -1.47010153611197, -0.698826068579047, -2.28219382856251, -4.78183085597533, -2.61566494476124, -1.33444106667307, -0.430021867171611, -0.294166317554753, -0.932391057274991, 0.172726295799422, -0.0873295379700724, -0.156114264651172, -0.542627889040196, 0.0395659889264757, -0.153028796529788, 239.93)
      val source6 = Seq(0.232512476136408, 0.938943680000523, -4.64777987861917, 3.07984368774547, -1.90265531863884, -1.04140751952949, -1.02040664482631, 0.547068603184495, -1.10599029357172, -3.52012767367189, 2.07411645462861, -3.01077173816878, 0.694977689554512, -5.38783122686352, 1.778669797797, -2.93580442477364, -2.28557270821558, -0.237055632781029, 1.97974707098703, 1.14161507545704, 0.911372759326949, 1.04292865643844, 0.999393887913078, 0.901260455221573, -0.452093168108936, 0.192959213794113, 0.180859449380341, -0.0293153378913553, 345)

      val result = Seq(0.0, 0.0, 0.0, 2.6201036, 0.0, 8.60147285, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.1242677, 0.0510884523, 0.0, 0.0, 2.31073856, 0.0, 5.34564304, 0.0, 5.33798933, 0.0, 0.0, 0.317612886, 2.49213839, 0.0, 17.4091167)
      val result1 = Seq(0.0, 0.0, 0.0, 1.82621026, 0.0, 2.30749226, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.188500285, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.41060162)
      val result2 = Seq(0.0, 0.0, 0.0, 1.11087918, 0.0, 2.90554, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0301823616, 1.01199019, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0338683128, 0.0, 0.0, 0.0, 0.0, 0.0, 5.86501598)
      val result3 = Seq(0.516343594, 0.0, 0.0, 0.648651719, 0.0, 0.131745577, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.127504706, 0.0661466122, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.338299513)

      val result4 = Seq(0.0, 4.14837217, 0.0, 2.45464, 0.643358231, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.506028175, 0.0, 1.91543365, 0.0, 0.0, 0.0, 0.0, 0.0, 0.416847765, 0.0, 0.43680191)
      val result5 = Seq(0.0, 0.0, 0.0, 3.75850201, 0.0, 8.45397663, 0.0, 2.40938878, 0.0, 0.0, 0.0, 0.0, 0.0, 2.81442595, 0.0, 0.0379809141, 0.0, 0.0, 3.10604525, 0.0, 2.15508, 0.0, 4.35908842, 0.0, 0.0, 0.25308305, 2.25639462, 0.0, 17.0174122)
      val result6 = Seq(0.0, 0.0, 0.0, 2.25999069, 0.0, 7.94868183, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.618395925, 0.0, 0.0, 0.0, 2.46317911, 0.0, 5.69847488, 0.0, 4.63979101, 0.0, 0.0, 0.212585151, 2.3240819, 0.0, 16.5237312)

      var error = 0.0
      println(s"Sizes : source = ${source.size}; result = ${result.size}")
      0 to source.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source(i) - result(i)), 2)
      }
      println(s"error ${error / source.size}")
      error = 0.0
      0 to source1.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source1(i) - result1(i)), 2)
      }
      println(s"error ${error / source1.size}")
      error = 0.0
      0 to source2.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source2(i) - result2(i)), 2)
      }
      println(s"error ${error / source2.size}")
      error = 0.0
      0 to source3.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source3(i) - result3(i)), 2)
      }
      println(s"error ${error / source3.size}")
      error = 0.0
      0 to source4.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source4(i) - result4(i)), 2)
      }
      println(s"error ${error / source4.size}")
      error = 0.0
      0 to source5.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source5(i) - result5(i)), 2)
      }
      println(s"error ${error / source5.size}")
      error = 0.0
      0 to source6.size - 1 foreach { i ⇒
        error = error + scala.math.pow((source6(i) - result6(i)), 2)
      }
      println(s"error ${error / source6.size}")
      succeed
    }
  }
}
