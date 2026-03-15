package todos.utils

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.{Exit, Runtime, Unsafe, ZIO}

trait CommonUtilsTest extends AnyFlatSpec with Matchers with MockFactory {
  protected def unsafeRun[A](zio: ZIO[Any, Throwable, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(zio).getOrThrow()
    }

  def checkSuccess[A](zio: ZIO[Any, Throwable, A])(expected: A): Unit = {
    val result = unsafeRun(zio.exit)

    result.isFailure shouldBe false
    result match {
      case Exit.Failure(cause) =>
        fail(s"Expected success, but got success with value ${cause.failureOption}")
      case Exit.Success(value) =>
        value shouldBe expected
    }
  }
  def checkFailure[A](zio: ZIO[Any, Throwable, A])(expected: Throwable): Unit = {
    val result = unsafeRun(zio.exit)
    result.isFailure shouldBe true
    result match {
      case Exit.Failure(cause) =>
        cause.failureOption shouldBe Some(expected)
      case Exit.Success(value) =>
        fail(s"Expected failure, but got success with value $value")
    }
  }

}
