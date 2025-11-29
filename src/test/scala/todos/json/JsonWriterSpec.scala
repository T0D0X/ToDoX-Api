package todos.json

import zio.json.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import todos.models.{CreateTodoRequest, Priority, TodoItem, UpdateTodoRequest}

import java.time.Instant
import java.util.UUID

class JsonWriterSpec extends AnyFlatSpec with Matchers {
		"UpdateTodoRequest" should "fullWriterTest" in {
				val request = UpdateTodoRequest(
						description = Some("Study ZIO"),
						priority = Some(Priority.Medium),
						isComplete = Some(true),
						completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
						tags = Some(List("work", "urgent")),
				)
				val json = request.toJson
				json.fromJson[UpdateTodoRequest] shouldBe Right(request)
		}

		it should "MinimalWriterTest" in {
				val request = UpdateTodoRequest(
						description = None,
						priority = None,
						isComplete = None,
						completeAt = None,
						tags = None,
				)
				val json = request.toJson
				json.fromJson[UpdateTodoRequest] shouldBe Right(request)
		}

		"CreateTodoRequest" should "fullWriterTest" in {
				val request = CreateTodoRequest(
						userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
						description = Some("Study ZIO"),
						priority = Priority.Medium,
						completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
						tags = List("work", "urgent"),
				)
				val json = request.toJson
				json.fromJson[CreateTodoRequest] shouldBe Right(request)
		}

		it should "MinimalWriterTest" in {
				val request = CreateTodoRequest(
						userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
						description = None,
						priority = Priority.Medium,
						completeAt = None,
						tags = List.empty,
				)
				val json = request.toJson
				json.fromJson[CreateTodoRequest] shouldBe Right(request)
		}

		"TodoItem" should "fullWriterTest" in {
				val request = TodoItem(
						userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
						id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
						description = Some("Study ZIO"),
						priority = Priority.Medium,
						isComplete = true,
						createAt = Instant.parse("2021-10-01T12:00:00Z"),
						completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
						tags = List("work", "urgent"),
				)
				val json = request.toJson
				json.fromJson[TodoItem] shouldBe Right(request)
		}

		it should "MinimalWriterTest" in {
				val request = TodoItem(
						userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
						id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
						description = None,
						priority = Priority.Medium,
						isComplete = true,
						createAt = Instant.parse("2021-10-01T12:00:00Z"),
						completeAt = None,
						tags = List.empty,
				)
				val json = request.toJson
				json.fromJson[TodoItem] shouldBe Right(request)
		}
}
