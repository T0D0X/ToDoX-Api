package todos.config

import scala.concurrent.duration.DurationInt

class RedisConfigSpec extends CommonReaderConfigTests[RedisConfig] {
  successTest(
    """
      |{
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
      |""".stripMargin,
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
