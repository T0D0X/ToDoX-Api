package todos.config

import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.*
import org.scalatest.matchers.should.Matchers

class AuthConfigSpec extends AnyFlatSpec with Matchers {
  it should "Success" in {
    val conf =
      """
        |auth {
        | auth-token = "token"
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("auth").load[AuthConfig]
    result shouldBe Right(
      AuthConfig("token"),
    )
  }

  it should "Fail" in {
    val conf =
      """
        | auth {
        |  authToken = "inc_token"
        | }
        |""".stripMargin

    val result = ConfigSource.string(conf).at("auth").load[AuthConfig]
    result.isLeft shouldBe true
  }
}
