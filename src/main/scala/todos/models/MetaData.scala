package todos.models

import java.time.LocalDateTime
import java.util.UUID
import doobie._

object MetaData {
		implicit val priorityMeta: Meta[Priority] =
				Meta[String].timap(Priority.fromString) {
						case Priority.Low => "low"
						case Priority.Medium => "medium"
						case Priority.High => "high"
				}

		implicit val uuidMeta: Meta[UUID] =
				Meta[String].timap(UUID.fromString)(_.toString)

		implicit val localDateTimeMeta: Meta[LocalDateTime] =
				Meta[String].timap(LocalDateTime.parse)(_.toString)

		implicit val tagsMeta: Meta[List[String]] =
				Meta[String].timap(_.split(',').toList.filter(_.nonEmpty))(_.mkString(","))
}
