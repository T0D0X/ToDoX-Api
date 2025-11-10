package todos.models

import doobie.Meta
import zio.json.*

sealed trait Priority

object Priority {

		def fromString(s: String): Priority = s.toLowerCase match {
				case "high" => High
				case "medium" => Medium
				case _ => Low
		}

		case object Low extends Priority

		case object Medium extends Priority

		case object High extends Priority

		implicit val codec: JsonCodec[Priority] = JsonCodec.string.transformOrFail(
				str => Right(fromString(str)),
				{
						case Low => "low"
						case Medium => "medium"
						case High => "high"
				}
		)

		implicit val priorityMeta: Meta[Priority] =
				Meta[String].timap(Priority.fromString) {
						case Priority.Low => "low"
						case Priority.Medium => "medium"
						case Priority.High => "high"
				}
}