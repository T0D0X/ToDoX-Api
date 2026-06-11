package todos.config

import scala.concurrent.duration.DurationInt

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class RedisConfigSpec extends AnyFlatSpec with Matchers {
  it should "Success" in {
    val conf =
      """
        |redis {
        | uri = "redis://localhost:6379"
        | user = "user"
        | password = "password"
        | ttl {
        |   default-ttl = 1 day
        |   custom-ttl {
        |     test = 5 seconds
        |   }
        | }
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("redis").load[RedisConfig]
    result shouldBe Right(
      RedisConfig(
        uri = "redis://localhost:6379",
        user = "user",
        password = "password",
        ttl = TtlConfig(
          defaultTtl = 1.day,
          customTtl = Map("test" -> 5.seconds),
        ),
      ),
    )
  }
}
