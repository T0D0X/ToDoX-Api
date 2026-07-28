package todos.config

class AuthConfigSpec extends CommonReaderConfigTests[AuthConfig] {

  successTest(
    conf = """
             |{
             | auth-token = "token"
             |}
             |""".stripMargin,
    expected = AuthConfig("token"),
  )
  failureTest(
    conf = """
             | {
             |  authToken = "inc_token"
             | }
             |""".stripMargin,
  )
}
