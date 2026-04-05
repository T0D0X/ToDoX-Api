package todos.repository.todoimpl

import todos.models.{TodoItem, UpdateTodoRequest}
import zio.Task

import java.util.UUID

trait TodoRepository {
  def getAllByUserId(userId: UUID): Task[List[TodoItem]]

  def getById(id: UUID): Task[Option[TodoItem]]

  def createTodoItem(item: TodoItem): Task[Unit]

  def updateTodoItem(id: UUID, updateRequest: UpdateTodoRequest): Task[Unit]

  def deleteTodoItem(id: UUID): Task[Unit]
}
