package com.fintrack.plugins

import com.fintrack.core.domain.ErrorResponse
import com.fintrack.core.logger
import com.fintrack.core.withContext
import core.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import io.ktor.server.response.*


fun Application.configureStatusPages() {
    install(StatusPages) {
        val log = logger<Application>()

        // Handle custom application exceptions
        exception<AppException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value,
                "errorCode" to cause.errorCode
            ).warn { "${cause.javaClass.simpleName}: ${cause.message}" }

            val status = when (cause) {
                is AuthenticationException -> HttpStatusCode.Unauthorized
                is UnauthorizedAccessException -> HttpStatusCode.Unauthorized
                is ValidationException -> HttpStatusCode.UnprocessableEntity
                is ResourceNotFoundException -> HttpStatusCode.NotFound
            }

            call.respond(
                status,
                ErrorResponse(
                    message = cause.message ?: "An error occurred",
                    errorCode = cause.errorCode
                )
            )
        }

        exception<RequestValidationException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info { "Validation failed: ${cause.reasons.joinToString()}" }

            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ErrorResponse(
                    message = cause.reasons.joinToString(". "),
                    errorCode = "VALIDATION_ERROR"
                )
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).warn { "Bad request: ${cause.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    message = cause.message ?: "Invalid request",
                    errorCode = "BAD_REQUEST"
                )
            )
        }

        exception<NoSuchElementException> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info { "Resource not found: ${cause.message}" }
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    message = cause.message ?: "Resource not found",
                    errorCode = "NOT_FOUND"
                )
            )
        }

        // Handle rate limit exceeded
        status(HttpStatusCode.TooManyRequests) { call, status ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value,
                "clientIp" to call.request.origin.remoteHost
            ).warn { "Rate limit exceeded" }

            call.respond(
                status,
                ErrorResponse(
                    message = "Rate limit exceeded",
                    errorCode = "TOO_MANY_REQUESTS"
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            log.withContext(
                "statusCode" to 404,
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info { "404 Not Found" }
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    message = "Resource not found",
                    errorCode = "NOT_FOUND"
                )
            )
        }

        status(HttpStatusCode.Unauthorized) { call, _ ->
            log.withContext(
                "statusCode" to 401,
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).info { "401 Unauthorized" }
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    message = "Unauthorized access",
                    errorCode = "UNAUTHORIZED"
                )
            )
        }

        exception<Throwable> { call, cause ->
            log.withContext(
                "path" to call.request.uri,
                "method" to call.request.httpMethod.value
            ).error({ "Unhandled exception: ${cause.message}" }, cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    message = "Internal server error",
                    errorCode = "INTERNAL_SERVER_ERROR"
                )
            )
        }
    }
}
