package todos.config

import pureconfig.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JwtConfigSpec extends AnyFlatSpec with Matchers {
  it should "Success" in {
    val conf =
      """
        |jwt {
        |  secret = "my-very-secret-key"
        |  issuer = "todo-app"
        |  ttl = 86400
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("jwt").load[JwtConfig]

    result shouldBe Right(
      JwtConfig(
        secret = "my-very-secret-key",
        issuer = "todo-app",
        ttl = 86400L,
      ),
    )
  }
  it should "Fail" in {
    val conf =
      """
        |jwt {
        |  secret = "secret"
        |  issuer = "todo-app"
        |  ttl = "not-a-number"
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("jwt").load[JwtConfig]

    result.isLeft shouldBe true
    result.left.toOption.get.toList.map(_.description).mkString should include("NUMBER")
  }

}
