package todos.config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.concurrent.duration.DurationInt

class RedisConfigSpec extends AnyFlatSpec with Matchers {
  it should "Success" in {
    val conf =
      """
        |redis {
        | uri = "redis://localhost:6379"
        | user = "user"
        | password = "password"
        | timeouts = {
        |   user = "10 seconds"
        | }
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("redis").load[RedisConfig]
    result shouldBe Right(
      RedisConfig(
        uri = "redis://localhost:6379",
        user = "user",
        password = "password",
        timeouts = Map("user" -> 10.seconds),
      ),
    )
  }
}
