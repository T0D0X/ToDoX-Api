package todos.service

import todos.errors.AppErrors.*
import todos.utils.ToDoGenerators.*
import todos.utils.CommonUtilsTest
import todos.models.{CreateTodoRequest, Priority, TodoItem, UpdateTodoRequest}
import todos.repository.TodoRepository
import zio.ZIO

import java.util.UUID

class TodoServiceTest extends CommonUtilsTest {

  "get" should "return TodoItem" in new Testing {
    todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))

    checkSuccess(service.get(todoItem.id))(Some(todoItem))
  }

  it should "return None" in new Testing {
    todoRepository.getById.expects(id).returns(ZIO.succeed(None))

    checkSuccess(service.get(id))(None)
  }

  "update" should "Success result" in new Testing {
    val todoUpdatePriority =
      todoUpdateEmpty.copy(priority = Some(Priority.High))

    todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))
    todoRepository.updateTodoItem.expects(todoItem.id, todoUpdatePriority).returns(ZIO.unit)

    checkSuccess(service.update(todoItem.id, todoUpdatePriority))(())
  }

  it should "Todo doesn't have" in new Testing {
    val todoUpdatePriority =
      todoUpdateEmpty.copy(priority = Some(Priority.High))

    todoRepository.getById.expects(id).returns(ZIO.succeed(None))

    checkFailure(service.update(id, todoUpdatePriority))(TodoNotFoundError(id.toString))
  }

  it should "doesn't update" in new Testing {
    todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))

    checkFailure(service.update(todoItem.id, todoUpdateEmpty))(EmptyFieldError())
  }

  "delete" should "Success operation" in new Testing {
    todoRepository.getById.expects(todoItem.id).returns(ZIO.some(todoItem))
    todoRepository.deleteTodoItem.expects(todoItem.id).returns(ZIO.unit)

    checkSuccess(service.delete(todoItem.id))(())
  }

  it should "Todo doesn't have" in new Testing {
    todoRepository.getById.expects(id).returns(ZIO.succeed(None))

    checkFailure(service.delete(id))(TodoNotFoundError(id.toString))
  }

  "create" should "Success operation" in new Testing {
    todoRepository.crateTodoItem.expects(*).returns(ZIO.unit)

    checkSuccess(service.create(todoCreate))(())
  }

  "getByUserId" should "Success operation" in new Testing {
    todoRepository.getAllByUserId.expects(*).returns(ZIO.succeed(List(todoItem)))

    checkSuccess(service.getByUserId(todoItem.userId))(List(todoItem))
  }

  trait Testing {
    val todoItem                       = generateUnsafe[TodoItem]
    val todoUpdateEmpty                = UpdateTodoRequest.empty
    val todoCreate                     = generateUnsafe[CreateTodoRequest].copy(userId = todoItem.userId)
    val id                             = generateUnsafe[UUID]
    val todoRepository: TodoRepository = mock[TodoRepository]
    val service                        = new TodoServiceImpl(todoRepository)
  }
}
