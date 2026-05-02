package todos.repository.userimpl

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import todos.models.UserData
import zio.Task
import zio.interop.catz.*

import java.util.UUID

class PostgresUserRepository(xa: Transactor[Task]) extends UserRepository {

  override def getByUserId(userId: UUID): Task[Option[UserData]] =
    sql"""
					SELECT user_id, login, email, phone, password_hash
					FROM users WHERE user_id = $userId
	"""
      .query[UserData]
      .option
      .transact(xa)

  override def getByLogin(login: String): Task[Option[UserData]] =
    sql"""
            SELECT user_id, login, email, phone, password_hash
            FROM users WHERE login = $login
        """
      .query[UserData]
      .option
      .transact(xa)

  override def createUser(user: UserData): Task[Boolean] =
    sql"""
					INSERT INTO users(
					user_id,
					login,
                    password_hash,
	                email,
                    phone
					) VALUES (
					${user.userId},
	                ${user.login},
                    ${user.passwordHash},
	                ${user.email},
                    ${user.phone}
					) ON CONFLICT (login) DO NOTHING
	""".update.run.transact(xa).map(_ == 1)

  override def deleteByUserId(userId: UUID): Task[Unit] =
    sql"""DELETE FROM users WHERE user_id = $userId""".update.run
      .transact(xa)
      .unit

  override def deleteByLogin(login: String): Task[Unit] =
    sql"""DELETE FROM users WHERE login = $login""".update.run
      .transact(xa)
      .unit
}
