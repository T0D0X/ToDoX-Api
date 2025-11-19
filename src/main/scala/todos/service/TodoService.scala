package todos.service

import todos.errors.AppErrors.*
import todos.models.{CreateTodoRequest, TodoItem, UpdateTodoRequest}
import todos.repository.TodoRepository
import zio.{Task, ZIO}

import java.util.UUID

trait TodoService {
		def get(id: UUID): Task[Option[TodoItem]]

		def update(id: UUID, updateRequest: UpdateTodoRequest): Task[Unit]

		def delete(id: UUID): Task[Unit]

		def create(createTodoRequest: CreateTodoRequest): Task[Unit]
}

class TodoServiceImpl(todoRepository: TodoRepository) extends TodoService {

		override def update(id: UUID, updateRequest: UpdateTodoRequest): Task[Unit] = for {
				existingItem <- get(id)

				_ <- existingItem match {
						case Some(_) => ZIO.unit
						case None => ZIO.fail(TodoNotFoundError(id.toString))
				}

				_ <- ZIO.when(
						List(
								updateRequest.description,
								updateRequest.priority,
								updateRequest.isComplete,
								updateRequest.completeAt,
								updateRequest.tags,
						).forall(_.isEmpty)
				)(ZIO.fail(EmptyFieldError()))

				_ <- todoRepository.updateTodoItem(id, updateRequest)
				.mapError(error => DatabaseOperationError("updating todo", error))

		} yield ()

		override def delete(id: UUID): Task[Unit] = for {
				existingItem <- get(id)

				_ <- existingItem match {
						case Some(_) => ZIO.unit
						case None => ZIO.fail(TodoNotFoundError(id.toString))
				}

				_ <- todoRepository.deleteTodoItem(id)
				.mapError(error => DatabaseOperationError("deleting todo", error))

		} yield ()

		override def get(id: UUID): Task[Option[TodoItem]] =
				todoRepository.getById(id)
				.mapError(error => DatabaseOperationError("get", error))

		override def create(createTodoRequest: CreateTodoRequest): Task[Unit] = ???
}
