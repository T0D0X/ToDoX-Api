package todos.common

import todos.errors.AppErrors.*

import java.util.UUID
import todos.models.{CreateTodoRequest, TodoItem, UpdateTodoRequest}
import todos.service.{JwtService, TodoService}
import zio.{IO, Task, ZIO}

object MockService {

  def mockTodoService(
      getResult: Task[Option[TodoItem]] = ZIO.none,
      createResult: Task[Unit] = ZIO.unit,
      updateResult: Task[Unit] = ZIO.unit,
      deleteResult: Task[Unit] = ZIO.unit,
      getByUserIdResult: Task[List[TodoItem]] = ZIO.succeed(List.empty[TodoItem]),
  ): TodoService = new TodoService {
    override def get(id: UUID): Task[Option[TodoItem]] = getResult
    override def create(request: CreateTodoRequest): Task[Unit] = createResult
    override def update(id: UUID, request: UpdateTodoRequest): Task[Unit] = updateResult
    override def delete(id: UUID): Task[Unit] = deleteResult
    override def getByUserId(userId: UUID): Task[List[TodoItem]] = getByUserIdResult
  }

  def mockJwtSrevice(
      token: String,
      uuidReturn: Option[UUID] = None,
      errorsReturn: Option[AuthErrorBase] = None,
  ): JwtService = new JwtService {
    override def generateToken(userId: UUID): Task[String] = ZIO.succeed(token)
    override def validateToken(token: String): IO[AuthErrorBase, UUID] =
      (uuidReturn, errorsReturn) match {
        case (Some(uuid), _) => ZIO.succeed(uuid)
        case (_, Some(error)) => ZIO.fail(error)
        case _ => ZIO.fail(InvalidTokenError("What test?!"))
      }
  }
}
