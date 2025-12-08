package todos.controller
//
//import todos.service.TodoService
//import zio.*
//import zio.test.*
//import zio.test.TestAspect.*
//import zio.test.Assertion.*
//import todos.generator.ToDoGenerators.*
//import zio.test.ZIOSpecDefault
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.matchers.should.Matchers
//import sttp.model.Method
//import todos.controller.TodoControllerSpec.{toMockFunction1, toStubFunction1}
//
//import java.util.UUID
//
//object TodoControllerSpec extends ZIOSpecDefault with MockFactory with Matchers {
//		val mockTodoService = mock[TodoService]
//		val controller = TodoController.make(mockTodoService)
//
//		def spec = suite("TodoController")(
//				getTodoEndpointSpec
//		)
//				
//		
//		private def getTodoEndpointSpec = suite("getTodoEndpoint")(
//						test("should match correct path") {
//								
//						}
//				),
//		)
//}
