package todos.json

import zio.json.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import todos.models.{CreateTodoRequest, Priority, TodoItem, UpdateTodoRequest}

import java.time.Instant
import java.util.UUID


class JsonReaderSpec extends AnyFlatSpec with Matchers {
		"UpdateTodoRequest" should "FullReadTest" in {
				val json =
						"""
							|{
							|     "description": "Study ZIO",
							|     "priority": "Medium",
							|			"isComplete": true,
							|			"completeAt": "2023-10-01T12:00:00Z",
							|			"tags": ["work", "urgent"]
							|}
							|""".stripMargin


				json.fromJson[UpdateTodoRequest] shouldBe Right(
						UpdateTodoRequest(
								description = Some("Study ZIO"),
								priority = Some(Priority.Medium),
								isComplete = Some(true),
								completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
								tags = Some(List("work", "urgent")),
						)
				)
		}

		it should "MinimalReadTest" in {
				val json =
						"""
							|{}
							|""".stripMargin


				json.fromJson[UpdateTodoRequest] shouldBe Right(
						UpdateTodoRequest(
								description = None,
								priority = None,
								isComplete = None,
								completeAt = None,
								tags = None,
						)
				)
		}

		"CreateTodoRequest" should "FullReadTest" in {
				val json =
						"""
							|{
							|     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
							|     "description": "Study ZIO",
							|     "priority": "Medium",
							|			"completeAt": "2023-10-01T12:00:00Z",
							|			"tags": ["work", "urgent"]
							|}
							|""".stripMargin


				json.fromJson[CreateTodoRequest] shouldBe Right(
						CreateTodoRequest(
								userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
								description = Some("Study ZIO"),
								priority = Priority.Medium,
								completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
								tags = List("work", "urgent"),
						)
				)
		}

		it should "MinimalReadTest" in {
				val json =
						"""
							|{
							|     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
							|     "priority": "low",
							|     "tags": []
							|}
							|""".stripMargin


				json.fromJson[CreateTodoRequest] shouldBe Right(
						CreateTodoRequest(
								userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
								description = None,
								priority = Priority.Low,
								completeAt = None,
								tags = List.empty,
						)
				)
		}

		"TodoItem" should "FullReadTest" in {
				val json =
						"""
							|{
							|     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
							|     "id": "00000000-0000-0000-0000-000000000000",
							|     "description": "Study ZIO",
							|     "priority": "Medium",
							|     "isComplete": true,
							|     "createAt": "2021-10-01T12:00:00Z",
							|			"completeAt": "2023-10-01T12:00:00Z",
							|			"tags": ["work", "urgent"]
							|}
							|""".stripMargin


				json.fromJson[TodoItem] shouldBe Right(
						TodoItem(
								userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
								id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
								description = Some("Study ZIO"),
								priority = Priority.Medium,
								isComplete = true,
								createAt = Instant.parse("2021-10-01T12:00:00Z"),
								completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
								tags = List("work", "urgent"),
						)
				)
		}

		it should "MinimalReadTest" in {
				val json =
						"""
							|{
							|     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
							|     "id": "00000000-0000-0000-0000-000000000000",
							|     "priority": "Medium",
							|     "isComplete": true,
							|     "createAt": "2021-10-01T12:00:00Z",
							|			"tags": []
							|}
							|""".stripMargin


				json.fromJson[TodoItem] shouldBe Right(
						TodoItem(
								userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
								id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
								description = None,
								priority = Priority.Medium,
								isComplete = true,
								createAt = Instant.parse("2021-10-01T12:00:00Z"),
								completeAt = None,
								tags = List.empty,
						)
				)
		}
}
