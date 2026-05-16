package todos.config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class ValidationConfigSpec extends AnyFlatSpec with Matchers {
  it should "Success" in {
    val conf =
      """validation {
        |  phone = "^(\\+7|8)\\d{10}$"
        |  email = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        |  password = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
        |}
        |""".stripMargin
    val result = ConfigSource.string(conf).at("validation").load[ValidationConfig]
    val config = result.getOrElse(fail("Parsing failed"))
    config.phone.regex shouldBe """^(\+7|8)\d{10}$"""
    config.email.regex shouldBe """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"""
    config.password.regex shouldBe """^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]).{8,}$"""
  }
}
