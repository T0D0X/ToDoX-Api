package todos.service

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import todos.errors.AppErrors.*
import todos.generator.ToDoGenerators.*
import todos.models.{CreateTodoRequest, Priority, TodoItem, UpdateTodoRequest}
import todos.repository.TodoRepository
import zio.{Exit, ZIO}
import zio.Unsafe
import zio.Runtime

import java.util.UUID


class TodoServiceTest extends AnyFlatSpec with Matchers with MockFactory {

		private def unsafeRun[A](zio: ZIO[Any, Throwable, A]): A =
				Unsafe.unsafe { implicit unsafe =>
						Runtime.default.unsafe.run(zio).getOrThrow()
				}

		"get" should "return TodoItem" in new Testing {
				todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))

				unsafeRun(service.get(todoItem.id)) shouldBe Some(todoItem)
		}

		it should "return None" in new Testing {
				todoRepository.getById.expects(id).returns(ZIO.succeed(None))

				unsafeRun(service.get(id)) shouldBe None
		}

		"update" should "Success result" in new Testing {
				val todoUpdatePriority = todoUpdateEmpty.copy(priority = Some(Priority.High))

				todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))
				todoRepository.updateTodoItem.expects(todoItem.id, todoUpdatePriority).returns(ZIO.unit)

				unsafeRun(service.update(todoItem.id, todoUpdatePriority)) shouldBe()
		}

		it should "Todo doesn't have" in new Testing {
				val todoUpdatePriority = todoUpdateEmpty.copy(priority = Some(Priority.High))

				todoRepository.getById.expects(id).returns(ZIO.succeed(None))

				val result = unsafeRun(service.update(id, todoUpdatePriority).exit)

				result.isFailure shouldBe true
				result match {
						case Exit.Failure(cause) =>
								cause.failureOption shouldBe Some(TodoNotFoundError(id.toString))
						case Exit.Success(_) =>
								fail("Expected failure but got success")
				}
		}

		it should "doesn't update" in new Testing {
				todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))

				val result = unsafeRun(service.update(todoItem.id, todoUpdateEmpty).exit)

				result.isFailure shouldBe true
				result match {
						case Exit.Failure(cause) =>
								cause.failureOption shouldBe Some(EmptyFieldError())
						case Exit.Success(_) =>
								fail("Expected failure but got success")
				}
		}

		"delete" should "Success operation" in new Testing {
				todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))
				todoRepository.deleteTodoItem.expects(todoItem.id).returns(ZIO.unit)

				unsafeRun(service.delete(todoItem.id)) shouldBe ()
		}

		it should "Todo doesn't have" in new Testing {
				todoRepository.getById.expects(id).returns(ZIO.succeed(None))

				val result = unsafeRun(service.delete(id).exit)

				result.isFailure shouldBe true
				result match {
						case Exit.Failure(cause) =>
								cause.failureOption shouldBe Some(TodoNotFoundError(id.toString))
						case Exit.Success(_) =>
								fail("Expected failure but got success")
				}
		}

		"create" should "Success operation" in new Testing {
				todoRepository.crateTodoItem.expects(*).returns(ZIO.unit)

				unsafeRun(service.create(todoCreate)) shouldBe()
		}


		trait Testing {
				val todoItem = generateUnsafe[TodoItem]
				val todoUpdateEmpty = UpdateTodoRequest.empty
				val todoCreate = generateUnsafe[CreateTodoRequest].copy(userId = todoItem.userId)
				val id = generateUnsafe[UUID]
				val todoRepository: TodoRepository = mock[TodoRepository]
				val service = new TodoServiceImpl(todoRepository)
		}
}
