package plugins

import com.fintrack.core.ApiResponse
import core.UnauthorizedAccessException
import core.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {

        // 400 Bad Request – invalid input or arguments
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.Error(cause.message ?: "Invalid request")
            )
        }

        // 404 Not Found – for missing resources
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse.Error(cause.message ?: "Resource not found")
            )
        }

        // 401 Unauthorized – for access control issues
        exception<UnauthorizedAccessException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse.Error(cause.message ?: "Unauthorized")
            )
        }

        // 422 Unprocessable Entity – validation errors
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ApiResponse.Error(cause.message ?: "Validation failed")
            )
        }

        // 500 Internal Server Error – fallback for any other uncaught exceptions
        exception<Throwable> { call, cause ->
            cause.printStackTrace() // log stack trace
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.Error("Internal server error")
            )
        }

        // handle HTTP status codes globally
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                ApiResponse.Error("Resource not found")
            )
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                ApiResponse.Error("Unauthorized access")
            )
        }
    }
}