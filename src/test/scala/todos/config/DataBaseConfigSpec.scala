package todos.config

class DataBaseConfigSpec extends CommonReaderConfigTests[DataBaseConfig] {

  successTest(
    conf = """
             |{
             | host = "localhost"
             | port = "8080"
             | name = "todo_test"
             | user = "test_user"
             | password = "test_password"
             | migration-enable = true
             |}
             |""".stripMargin,
    expected = DataBaseConfig(
      host = "localhost",
      port = 8080,
      name = "todo_test",
      user = "test_user",
      password = "test_password",
      migrationEnable = true,
    ),
  )

  failureWithMessageTest(
    conf = """
             |{
             | host = "localhost"
             | port = 8080
             | user = "test_user"
             | password = "test_password"
             |}
             |""".stripMargin,
    expectedError = "name",
  )
}
