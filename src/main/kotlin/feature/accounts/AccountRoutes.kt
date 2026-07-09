package com.fintrack.feature.accounts

import com.fintrack.core.domain.ApiResponse
import com.fintrack.core.domain.ErrorResponse
import com.fintrack.core.domain.Result
import com.fintrack.core.domain.toHttpStatusCode
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.domain.AccountService
import core.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.accountsRoutes(accountService: AccountService) {
    route("/accounts") {

        get {
            val userId = call.userIdOrThrow()
            when (val result = accountService.getAllAccounts(userId)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        get("/{id}") {
            val accountId = call.parameters["id"]?.toUUIDOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            when (val result = accountService.getAccount(userId, accountId)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateAccountRequest>()
            when (val result = accountService.createAccount(userId, request)) {
                is Result.Success -> call.respond(HttpStatusCode.Created, ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        put("/{id}") {
            val accountId = call.parameters["id"]?.toUUIDOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            val request = call.receive<UpdateAccountRequest>()
            when (val result = accountService.updateAccount(userId, accountId, request)) {
                is Result.Success -> call.respond(ApiResponse.Success(result.value))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }

        delete("/{id}") {
            val accountId = call.parameters["id"]?.toUUIDOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            when (val result = accountService.deleteAccount(userId, accountId)) {
                is Result.Success -> call.respond(ApiResponse.Success("Account deleted successfully"))
                is Result.Failure -> call.respond(
                    result.error.toHttpStatusCode(),
                    ErrorResponse(result.error.message, result.error.errorCode)
                )
            }
        }
    }
}
