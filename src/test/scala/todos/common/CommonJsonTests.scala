package todos.common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.json.*

trait CommonJsonTests extends AnyFlatSpec with Matchers {

  def testRead[A](
      desc: String = "ReadTest",
      json: String,
      obj: A,
  )(implicit decoder: JsonDecoder[A]): Unit =
    it should s"${obj.getClass.getSimpleName} : $desc" in {
      json.fromJson[A] shouldBe Right(obj)
    }

  def testWrite[A](
      desc: String = "WriteTest",
      json: String,
      obj: A,
  )(implicit encoder: JsonEncoder[A]): Unit =
    it should s"${obj.getClass} : $desc" in {
      normalize(obj.toJson) shouldBe normalize(json)
    }

  private def normalize(s: String): String = s.replaceAll("\\s+", "")
}
