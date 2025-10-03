package plugins

import com.fintrack.core.ApiResponse
import com.fintrack.core.error
import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.core.withContext
import core.AuthenticationException
import core.UnauthorizedAccessException
import core.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.*
fun Application.configureStatusPages() {
    install(StatusPages) {
        val log = this@configureStatusPages.logger<Application>()

        exception<IllegalArgumentException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).warn("Bad request: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.Error(cause.message ?: "Invalid request")
            )
        }

        exception<AuthenticationException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).warn("Authentication failed: ${cause.message}")
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse.Error(cause.message ?: "Authentication failed")
            )
        }

        exception<NoSuchElementException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info("Resource not found: ${cause.message}")
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse.Error(cause.message ?: "Resource not found")
            )
        }

        exception<UnauthorizedAccessException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).warn("Unauthorized access: ${cause.message}")
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse.Error(cause.message ?: "Unauthorized")
            )
        }

        exception<ValidationException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info("Validation failed: ${cause.message}")
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ApiResponse.Error(cause.message ?: "Validation failed")
            )
        }

        exception<Throwable> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).error("Unhandled exception: ${cause.message}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.Error("Internal server error")
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            log.withContext(
                "statusCode" to 404,
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info("404 Not Found")
            call.respond(ApiResponse.Error("Resource not found"))
        }

        status(HttpStatusCode.Unauthorized) { call, status ->
            log.withContext(
                "statusCode" to 401,
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info("401 Unauthorized")
            call.respond(ApiResponse.Error("Unauthorized access"))
        }
    }
}