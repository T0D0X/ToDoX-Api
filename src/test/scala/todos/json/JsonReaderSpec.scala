package todos.json

import todos.models.{CreateTodoRequest, Priority, TodoItem, UpdateTodoRequest}
import todos.common.CommonJsonTests

import java.time.Instant
import java.util.UUID

class JsonReaderSpec extends CommonJsonTests {

  testRead(
    desc = "FullReadTest",
    json = """
             |{
             |     "description": "Study ZIO",
             |     "priority": "Medium",
             |	   "isComplete": true,
             |	   "completeAt": "2023-10-01T12:00:00Z",
             |	   "tags": ["work", "urgent"]
             |}
             |""".stripMargin,
    obj = UpdateTodoRequest(
      description = Some("Study ZIO"),
      priority = Some(Priority.Medium),
      isComplete = Some(true),
      completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
      tags = Some(List("work", "urgent")),
    ),
  )

  testRead(
    desc = "MinimalReadTest",
    json = """
             |{}
             |""".stripMargin,
    obj = UpdateTodoRequest.empty,
  )

  testRead(
    desc = "FullReadTest",
    json = """
             |{
             |     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
             |     "description": "Study ZIO",
             |     "priority": "Medium",
             |	   "completeAt": "2023-10-01T12:00:00Z",
             |	   "tags": ["work", "urgent"]
             |}
             |""".stripMargin,
    obj = CreateTodoRequest(
      userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
      description = Some("Study ZIO"),
      priority = Priority.Medium,
      completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
      tags = List("work", "urgent"),
    ),
  )

  testRead(
    desc = "MinimalReadTest",
    json = """
             |{
             |     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
             |     "priority": "low",
             |     "tags": []
             |}
             |""".stripMargin,
    obj = CreateTodoRequest(
      userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
      description = None,
      priority = Priority.Low,
      completeAt = None,
      tags = List.empty,
    ),
  )

  testRead(
    desc = "FullReadTest",
    json = """
             |{
             |     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
             |     "id": "00000000-0000-0000-0000-000000000000",
             |     "description": "Study ZIO",
             |     "priority": "medium",
             |     "isComplete": true,
             |     "createAt": "2021-10-01T12:00:00Z",
             |	   "completeAt": "2023-10-01T12:00:00Z",
             |	   "tags": ["work", "urgent"]
             |}
             |""".stripMargin,
    obj = TodoItem(
      userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
      id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      description = Some("Study ZIO"),
      priority = Priority.Medium,
      isComplete = true,
      createAt = Instant.parse("2021-10-01T12:00:00Z"),
      completeAt = Some(Instant.parse("2023-10-01T12:00:00Z")),
      tags = List("work", "urgent"),
    ),
  )

  testRead(
    desc = "MinimalReadTest",
    json = """
             |{
             |     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
             |     "id": "00000000-0000-0000-0000-000000000000",
             |     "priority": "medium",
             |     "isComplete": true,
             |     "createAt": "2021-10-01T12:00:00Z",
             |	   "tags": []
             |}
             |""".stripMargin,
    obj = TodoItem(
      userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
      id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
      description = None,
      priority = Priority.Medium,
      isComplete = true,
      createAt = Instant.parse("2021-10-01T12:00:00Z"),
      completeAt = None,
      tags = List.empty,
    ),
  )

//  testRead(
//    json = """
//             |{
//             |   "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
//             |   "login": "SlavaBuchnev",
//             |   "email": "slavabuchnev@pochta.ru",
//             |   "phone": "+799999999"
//             |}
//             |""".stripMargin,
//    obj = UserData(
//      userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
//      login = "SlavaBuchnev",
//      email = "slavabuchnev@pochta.ru",
//      phone = "+799999999",
//    ),
//  )
}
