package todos.json

import todos.models.{Priority, TodoItem}
import todos.common.CommonJsonTests

import java.time.Instant
import java.util.UUID

class JsonWriterSpec extends CommonJsonTests {

  testWrite(
    desc = "fullWriterTest",
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

  testWrite(
    desc = "MinimalWriterTest",
    json = """
             |{
             |     "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
             |     "id": "00000000-0000-0000-0000-000000000000",
             |     "priority": "medium",
             |     "isComplete": true,
             |     "createAt": "2021-10-01T12:00:00Z",
             |	  "tags": []
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

//  testWrite(
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
