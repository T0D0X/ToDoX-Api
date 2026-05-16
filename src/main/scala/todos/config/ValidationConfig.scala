package todos.config

import pureconfig.{ConfigReader, ConfigSource}
import sttp.tapir.ValidationResult
import sttp.tapir.ValidationResult.{Invalid, Valid}
import zio.{ZIO, ZLayer}

import scala.util.Try
import scala.util.matching.Regex

case class ValidationConfig(
    phone: Regex,
    email: Regex,
    password: Regex,
) derives ConfigReader {
  def validatePhone(value: String): ValidationResult =
    if (phone.matches(value)) Valid
    else Invalid(List(s"Invalid phone: $value. Expected: ${phone.regex}"))

  def validateEmail(value: String): ValidationResult =
    if (email.matches(value)) Valid
    else Invalid(List(s"Invalid email: $value. Expected: ${email.regex}"))

  def validatePassword(value: String): ValidationResult =
    if (password.matches(value)) Valid
    else Invalid(List(s"Invalid password: $value. Expected: ${password.regex}"))

}

object ValidationConfig {
  given ConfigReader[Regex] = ConfigReader.fromStringTry(str => Try(str.r))

  val live = ZLayer.fromZIO(ZIO.attempt(ConfigSource.default.at("validation").loadOrThrow[ValidationConfig]))
}
