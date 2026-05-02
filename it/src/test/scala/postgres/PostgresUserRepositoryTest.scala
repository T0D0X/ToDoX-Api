package postgres

import doobie.*
import doobie.implicits.*
import todos.config.DataBaseConfig
import todos.models.UserData
import todos.repository.{PostgresUserRepository, UserRepository}
import todos.common.ToDoGenerators.*
import zio.*
import zio.interop.catz.*
import zio.test.*
import zio.test.TestAspect.*

import java.util.UUID

object PostgresUserRepositoryTest extends ZIOSpecDefault {
  val transactorLayer: ZLayer[Any, Throwable, UserRepository] =
    ZLayer.scoped {
      for {
        xa <- DataBaseConfig.usersL
        _ <- ZIO.addFinalizer(cleanDatabase(xa).orDie)
      } yield new PostgresUserRepository(xa)
    }

  override def spec = suite("PostgresUserRepositoryTest")(
    createUserSpec,
    getByUserIdSpec,
    getByLoginSpec,
    deleteByUserIdSpec,
    deleteByLoginSpec,
  ) @@ sequential provideLayer transactorLayer

  def createUserSpec = suite("createUser")(
    test("success user create") {
      for {
        repo <- ZIO.service[UserRepository]
        user <- generate[UserData]
        result <- repo.createUser(user)
      } yield assertTrue(result)
    },
    test("user was created") {
      for {
        repo <- ZIO.service[UserRepository]
        user <- generate[UserData]
        before <- repo.createUser(user)
        after <- repo.createUser(user)
      } yield assertTrue(before && !after)
    },
  )

  def getByUserIdSpec = suite("getByUserId")(
    test("return Empty") {
      for {
        repo <- ZIO.service[UserRepository]
        userId <- generate[UUID]
        result <- repo.getByUserId(userId)
      } yield assertTrue(result.isEmpty)
    },
    test("return user") {
      for {
        repo <- ZIO.service[UserRepository]
        user <- generate[UserData]
        create <- repo.createUser(user)
        result <- repo.getByUserId(user.userId)
      } yield assertTrue(create && result.contains(user))
    },
  )

  def getByLoginSpec = suite("getByLogin")(
    test("return Empty") {
      for {
        repo <- ZIO.service[UserRepository]
        userId <- generate[UUID]
        result <- repo.getByUserId(userId)
      } yield assertTrue(result.isEmpty)
    },
    test("return user") {
      for {
        repo <- ZIO.service[UserRepository]
        user <- generate[UserData]
        create <- repo.createUser(user)
        result <- repo.getByLogin(user.login)
      } yield assertTrue(create && result.contains(user))
    },
  )

  def deleteByUserIdSpec = suite("deleteByUserId")(
    test("no user") {
      for {
        repo <- ZIO.service[UserRepository]
        userId <- generate[UUID]
        before <- repo.getByUserId(userId)
        _ <- repo.deleteByUserId(userId)
        after <- repo.getByUserId(userId)
      } yield assertTrue(before.isEmpty && after.isEmpty)
    },
    test("success delete user by userId") {
      for {
        repo <- ZIO.service[UserRepository]
        user <- generate[UserData]
        create <- repo.createUser(user)
        before <- repo.getByUserId(user.userId)
        _ <- repo.deleteByUserId(user.userId)
        after <- repo.getByUserId(user.userId)
      } yield assertTrue(create && before.nonEmpty && after.isEmpty)
    },
  )

  def deleteByLoginSpec = suite("deleteByLogin")(
    test("no user") {
      for {
        repo <- ZIO.service[UserRepository]
        login <- generate[String]
        before <- repo.getByLogin(login)
        _ <- repo.deleteByLogin(login)
        after <- repo.getByLogin(login)
      } yield assertTrue(before.isEmpty && after.isEmpty)
    },
    test("success delete user by login") {
      for {
        repo <- ZIO.service[UserRepository]
        user <- generate[UserData]
        create <- repo.createUser(user)
        before <- repo.getByLogin(user.login)
        _ <- repo.deleteByLogin(user.login)
        after <- repo.getByLogin(user.login)
      } yield assertTrue(create && before.nonEmpty && after.isEmpty)
    },
  )
  private def cleanDatabase(xa: Transactor[Task]): Task[Unit] =
    sql"TRUNCATE TABLE users".update.run.transact(xa).unit
}
