package todos.util

import zio.{Task, ZIO}
import com.outr.scalapass.{Argon2PasswordFactory, PasswordFactory}

object HashingUtil {

  private val factory: PasswordFactory = Argon2PasswordFactory()

  def hash(password: String): Task[String] =
    ZIO.attempt(factory.hash(password))

  def verify(password: String, hash: String): Task[Boolean] =
    ZIO.attempt(factory.verify(password, hash))
}
