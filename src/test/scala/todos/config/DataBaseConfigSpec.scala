package todos.config

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataBaseConfigSpec extends AnyFlatSpec with Matchers {

  private def validate(config: Config): Either[String, Unit] =
    try {
      DataBaseConfig(
        host = config.getString("host"),
        port = config.getInt("port"),
        name = config.getString("name"),
        user = config.getString("user"),
        password = config.getString("password"),
      )
      Right(())
    } catch {
      case e: ConfigException => Left(e.getMessage)
    }

  it should "Success" in {
    val conf =
      """
        |db {
        | host = "localhost"
        | port = "8080"
        | name = "todo_test"
        | user = "test_user"
        | password = "test_password"
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(conf).getConfig("db")

    validate(config) shouldBe Right(())
  }

  it should "Fail" in {
    val conf =
      """
        |db {
        | host = "localhost"
        | port = "8080"
        | user = "test_user"
        | password = "test_password"
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(conf).getConfig("db")
    validate(config) shouldBe Left("String: 2: No configuration setting found for key 'name'")
  }
}
