package com.fintrack.feature.accounts

import com.fintrack.core.domain.ApiResponse
import com.fintrack.core.toUUIDOrNull
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.domain.AccountService
import core.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.accountsRoutes(accountService: AccountService) {
    route("/accounts") {

        get {
            val userId = call.userIdOrThrow()
            val accounts = accountService.getAllAccounts(userId)
            call.respond(ApiResponse.Success(accounts))
        }

        get("/{id}") {
            val accountId = call.parameters["id"]?.toUUIDOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            val account = accountService.getAccount(userId, accountId)
            call.respond(ApiResponse.Success(account))
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateAccountRequest>()
            val account = accountService.createAccount(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(account))
        }

        put("/{id}") {
            val accountId = call.parameters["id"]?.toUUIDOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            val request = call.receive<UpdateAccountRequest>()
            val updatedAccount = accountService.updateAccount(userId, accountId, request)
            call.respond(ApiResponse.Success(updatedAccount))
        }

        delete("/{id}") {
            val accountId = call.parameters["id"]?.toUUIDOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            accountService.deleteAccount(userId, accountId)
            call.respond(ApiResponse.Success("Account deleted successfully"))
        }
    }
}
