package todos.config

class JwtConfigSpec extends CommonReaderConfigTests[JwtConfig] {
  successTest(
    """
      |{
      |  secret = "my-very-secret-key"
      |  issuer = "todo-app"
      |  ttl = 86400
      |}
      |""".stripMargin,
    JwtConfig(
      secret = "my-very-secret-key",
      issuer = "todo-app",
      ttl = 86400L,
    ),
  )
  failureWithMessageTest(
    """
      |{
      |  secret = "secret"
      |  issuer = "todo-app"
      |  ttl = "not-a-number"
      |}
      |""".stripMargin,
    "NUMBER",
  )
}
