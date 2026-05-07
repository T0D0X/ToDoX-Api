package todos.repository.userimpl

import todos.models.UserData
import zio.Task

import java.util.UUID

trait UserRepository {
  def getByUserId(userId: UUID): Task[Option[UserData]]
  def getByLogin(login: String): Task[Option[UserData]]
  def createUser(userData: UserData): Task[Boolean]
  def deleteByUserId(userId: UUID): Task[Unit]
  def deleteByLogin(login: String): Task[Unit]
}
