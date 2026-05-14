package todos.config

import pureconfig.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataBaseConfigSpec extends AnyFlatSpec with Matchers {

  it should "Success" in {
    val conf =
      """
        |db {
        | host = "localhost"
        | port = "8080"
        | name = "todo_test"
        | user = "test_user"
        | password = "test_password"
        | migration-enable = true
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("db").load[DataBaseConfig]

    result shouldBe Right(
      DataBaseConfig(
        host = "localhost",
        port = 8080,
        name = "todo_test",
        user = "test_user",
        password = "test_password",
        migrationEnable = true,
      ),
    )
  }

  it should "Fail" in {
    val conf =
      """
        |db {
        | host = "localhost"
        | port = 8080
        | user = "test_user"
        | password = "test_password"
        |}
        |""".stripMargin

    val result = ConfigSource.string(conf).at("db").load[DataBaseConfig]

    result.isLeft shouldBe true
    result.left.toOption.get.toList.map(_.description).mkString should include("name")
  }
}
