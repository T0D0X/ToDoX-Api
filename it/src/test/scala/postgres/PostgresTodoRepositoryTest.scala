package postgres

import doobie.*
import doobie.implicits.*
import todos.config.DataBaseConfig
import todos.models.*
import todos.repository.todoimpl.{PostgresTodoRepository, TodoRepository}
import todos.common.ToDoGenerators.*
import zio.*
import zio.interop.catz.*
import zio.test.*
import zio.test.TestAspect.*

import java.time.Instant
import java.util.UUID

object PostgresTodoRepositoryTest extends ZIOSpecDefault {

  val transactorLayer: ZLayer[Any, Throwable, TodoRepository] =
    ZLayer.scoped {
      for {
        xa <- DataBaseConfig.todoL
        _ <- ZIO.addFinalizer(cleanDatabase(xa).orDie)
      } yield new PostgresTodoRepository(xa)
    }
  override def spec = suite("PostgresTodoRepositoryTest")(
    getAllByUserIdSpec,
    getByIdSpec,
    deleteTodoItemSpec,
    updateTodoItemSpec,
  ) @@ sequential provideLayer transactorLayer

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
        _ <- repo.createTodoItem(todos)
        result <- repo.getAllByUserId(todos.userId)
      } yield assertTrue(result.size == 1 && result.head == todos)
    },
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
        _ <- repo.createTodoItem(todos)
        result <- repo.getById(todos.id)
      } yield assertTrue(result.contains(todos))
    },
  )

  def deleteTodoItemSpec = suite("deleteTodoItem")(
    test("check delete") {
      for {
        repo <- ZIO.service[TodoRepository]
        todos <- generate[TodoItem]
        _ <- repo.createTodoItem(todos)
        before <- repo.getById(todos.id)
        _ <- repo.deleteTodoItem(todos.id)
        after <- repo.getById(todos.id)
      } yield assertTrue(before.contains(todos) && after.isEmpty)
    },
  )

  def updateTodoItemSpec = suite("updateTodoItem")(
    test("update") {
      for {
        repo <- ZIO.service[TodoRepository]
        todos <- generate[TodoItem]
        newTodos <- generate[TodoItem]
        _ <- repo.createTodoItem(todos)
        request = UpdateTodoRequest(
          newTodos.description,
          Some(newTodos.priority),
          Some(newTodos.isComplete),
          newTodos.completeAt,
          Some(newTodos.tags),
        )
        _ <- repo.updateTodoItem(todos.id, request)
        result <- repo.getAllByUserId(todos.userId)
      } yield assertTrue(
        result.size == 1 &&
          result.head.id == todos.id &&
          result.head.userId == todos.userId &&
          newTodos.description.forall(sub => result.head.description.exists(_.contains(sub))) &&
          result.head.priority == newTodos.priority &&
          result.head.isComplete == newTodos.isComplete &&
          result.head.tags == newTodos.tags,
      )
    },
    test("update zero") {
      for {
        repo <- ZIO.service[TodoRepository]
        todos <- generate[TodoItem]
        newTodos <- generate[TodoItem]
        _ <- repo.createTodoItem(todos)
        request = UpdateTodoRequest.empty
        _ <- repo.updateTodoItem(todos.id, request)
        result <- repo.getAllByUserId(todos.userId)
      } yield assertTrue(
        result.size == 1 &&
          result.head.id == todos.id &&
          result.head.userId == todos.userId &&
          todos.description.forall(sub => result.head.description.exists(_.contains(sub))) &&
          result.head.priority == todos.priority &&
          result.head.isComplete == todos.isComplete &&
          result.head.tags == todos.tags,
      )
    },
    test("update desc and completeAt") {
      for {
        repo <- ZIO.service[TodoRepository]
        todos <- generate[TodoItem]
        time <- generate[Instant]
        _ <- repo.createTodoItem(todos)
        request = UpdateTodoRequest.empty.copy(
          description = Some("aaa"),
          completeAt = Some(time),
        )
        _ <- repo.updateTodoItem(todos.id, request)
        result <- repo.getAllByUserId(todos.userId)
      } yield assertTrue(
        result.size == 1 &&
          result.head.id == todos.id &&
          result.head.userId == todos.userId &&
          request.description.forall(sub => result.head.description.exists(_.contains(sub))) &&
          result.head.priority == todos.priority &&
          result.head.isComplete == todos.isComplete &&
          result.head.tags == todos.tags,
      )
    },
  )

  private def cleanDatabase(xa: Transactor[Task]): Task[Unit] =
    sql"TRUNCATE TABLE todo_items".update.run.transact(xa).unit
}
