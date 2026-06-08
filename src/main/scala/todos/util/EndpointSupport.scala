package todos.util

import todos.errors.{AppError, ErrorResponse}

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

import java.time.Instant

object EndpointSupport {

  val notFoundOutput: EndpointOutput[ErrorResponse] =
    jsonBody[ErrorResponse]
      .description("Ресурс не найден")
      .example(ErrorResponse("NotFoundError", "NOT_FOUND", "Запрашиваемый ресурс не найден", Instant.now()))

  val validationOutput: EndpointOutput[ErrorResponse] =
    jsonBody[ErrorResponse]
      .description("Ошибка валидации")
      .example(ErrorResponse("ValidationError", "VALIDATION", "Поля запроса заполнены некорректно", Instant.now()))

  val databaseOutput: EndpointOutput[ErrorResponse] =
    jsonBody[ErrorResponse]
      .description("Ошибка базы данных")
      .example(ErrorResponse("DatabaseError", "DB", "Внутренняя ошибка базы данных", Instant.now()))

  val unauthorizedOutput: EndpointOutput[ErrorResponse] =
    jsonBody[ErrorResponse]
      .description("Ошибка авторизации")
      .example(ErrorResponse("Unauthorized", "AUTH", "Невалидный или отсутствующий токен", Instant.now()))

  val defaultErrorOutput: EndpointOutput[ErrorResponse] =
    statusCode(StatusCode.BadRequest).and(jsonBody[ErrorResponse])

  /** Преобразует любое исключение в стандартизированный ErrorResponse */
  def toErrorResponse(throwable: Throwable): ErrorResponse = throwable match {
    case error: AppError => ErrorResponse.fromAppError(error)
    case ex: Throwable => ErrorResponse("InternalError", "UNKNOWN", ex.getMessage)
  }

  /** Стандартный набор ошибок для большинства эндпоинтов */
  val standardErrorOut: EndpointOutput.OneOf[ErrorResponse, ErrorResponse] =
    oneOf[ErrorResponse](
      oneOfVariantValueMatcher(StatusCode.NotFound, notFoundOutput) { case ErrorResponse(_, code, _, _) =>
        code.startsWith("NOT_FOUND")
      },
      oneOfVariantValueMatcher(StatusCode.UnprocessableEntity, validationOutput) { case ErrorResponse(_, code, _, _) =>
        code.startsWith("VALIDATION")
      },
      oneOfVariantValueMatcher(StatusCode.InternalServerError, databaseOutput) { case ErrorResponse(_, code, _, _) =>
        code.startsWith("DB")
      },
      oneOfVariantValueMatcher(StatusCode.Unauthorized, unauthorizedOutput) { case ErrorResponse(_, code, _, _) =>
        code.startsWith("AUTH")
      },
      oneOfDefaultVariant(defaultErrorOutput),
    )
}
