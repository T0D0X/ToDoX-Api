package todos.common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Assertion
import pureconfig.*

trait CommonReaderConfigTests[T: ConfigReader] extends AnyFlatSpec with Matchers {

  private def loadConfig(conf: String): ConfigReader.Result[T] =
    ConfigSource.string(conf).load[T]

  def successTest(conf: String, expected: T): Unit =
    it should "Success" in {
      val result = loadConfig(conf)
      result.isRight shouldBe true
      result shouldBe Right(expected)
    }

  def failureTest(conf: String): Unit =
    it should "Failure" in {
      val result = loadConfig(conf)
      result.isLeft shouldBe true
    }

  def failureWithMessageTest(conf: String, expectedError: String): Unit =
    it should "Failure with Message" in {
      val result = loadConfig(conf)
      result.isLeft shouldBe true
      result match {
        case Left(failures) =>
          val message = failures.toList.map(_.description).mkString(", ")
          message should include(expectedError)
        case Right(_) => fail("Expected failure but got success")
      }
    }
}
