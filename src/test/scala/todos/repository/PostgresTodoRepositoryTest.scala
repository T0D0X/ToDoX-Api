package todos.repository

import todos.generator.ToDoGenerators.*
import zio.*
import zio.test.*
import doobie.*
import doobie.implicits.*
import zio.interop.catz.*
import zio.test.TestAspect.*
import todos.models.*

import java.util.{Properties, UUID}

object PostgresTodoRepositoryTest extends ZIOSpecDefault {

		val transactorLayer: ZLayer[Any, Throwable, Transactor[Task]] =
				ZLayer.scoped {
						for {
								xa <- ZIO.succeed(createTestTransactor)
								_ <- ZIO.addFinalizer(cleanDatabase(xa).orDie)
						} yield xa
				}
		val repositoryLayer: ZLayer[Any, Throwable, TodoRepository] =
				transactorLayer >>> ZLayer.fromFunction(new PostgresTodoRepository(_))

		override def spec = suite("PostgresTodoRepositoryTest")(
				getAllByUserIdSpec,
				getByIdSpec,
				deleteTodoItemSpec,
				updateTodoItemSpec,
		) @@ sequential provideLayer repositoryLayer

		def getAllByUserIdSpec = suite("getAllByUserId")(
				test("getAllByUserId return Empty List") {
						for {
								repo <- ZIO.service[TodoRepository]
								userId <- generate[UUID]
								result <- repo.getAllByUserId(userId)
						} yield assertTrue(result.isEmpty)
				},
				test("getAllByUserId return 1 todos") {
						for {
								repo <- ZIO.service[TodoRepository]
								todos <- generate[TodoItem]
								_ <- repo.crateTodoItem(todos)
								result <- repo.getAllByUserId(todos.userId)
						} yield assertTrue(result.size == 1 && result.head == todos)
				}
		)

		def getByIdSpec = suite("getById")(
				test("getById return Empty") {
						for {
								repo <- ZIO.service[TodoRepository]
								id <- generate[UUID]
								result <- repo.getById(id)
						} yield assertTrue(result.isEmpty)
				},
				test("getById return 1 todos") {
						for {
								repo <- ZIO.service[TodoRepository]
								todos <- generate[TodoItem]
								_ <- repo.crateTodoItem(todos)
								result <- repo.getById(todos.id)
						} yield assertTrue(result.contains(todos))
				}
		)

		def deleteTodoItemSpec = suite("deleteTodoItem")(
				test("check delete") {
						for {
								repo <- ZIO.service[TodoRepository]
								todos <- generate[TodoItem]
								_ <- repo.crateTodoItem(todos)
								before <- repo.getById(todos.id)
								_ <- repo.deleteTodoItem(todos.id)
								after <- repo.getById(todos.id)
						} yield assertTrue(before.contains(todos) && after.isEmpty)
				}
		)

		def updateTodoItemSpec = suite("updateTodoItem")(
				test("update") {
						for {
								repo <- ZIO.service[TodoRepository]
								todos <- generate[TodoItem]
								_ <- repo.crateTodoItem(todos)
								request = UpdateTodoRequest(Some("aaa"), None, None, None, None)
								_ <- repo.updateTodoItem(todos.id, request)
								result <- repo.getAllByUserId(todos.userId)
						} yield assertTrue(result.size == 1 &&
						result.head.description.contains("aaa") &&
						result.head.priority == todos.priority &&
						result.head.isComplete == todos.isComplete &&
						result.head.tags == todos.tags
						)
				}
		)

		private def cleanDatabase(xa: Transactor[Task]): Task[Unit] =
				sql"TRUNCATE TABLE todo_items".update.run.transact(xa).unit

		private def createTestTransactor: Transactor[Task] = {
				val props = new Properties()
				props.setProperty("user", "test_user")
				props.setProperty("password", "test_password")

				Transactor.fromDriverManager[Task](
						driver = "org.postgresql.Driver",
						url = "jdbc:postgresql://localhost:8080/todo_test",
						info = props,
						logHandler = None,
				)
		}
}