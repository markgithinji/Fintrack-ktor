package feature.transaction


import com.fintrack.core.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.data.model.AccountDto
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
import java.util.UUID

fun Route.accountsRoutes(accountService: AccountService) {
    val log = logger("AccountRoutes")

    route("/accounts") {

        get {
            val userId = call.userIdOrThrow()
            log.withContext("userId" to userId, "endpoint" to "GET /accounts")
                .info { "Fetching all accounts request received" }

            val accounts: List<AccountDto> = accountService.getAllAccounts(userId)
            call.respond(ApiResponse.Success(accounts))
        }

        get("/{id}") {
            val accountId = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "GET /accounts/{id}"
            ).info { "Fetching account request received" }

            val account: AccountDto = accountService.getAccount(userId, accountId)
                ?: throw NoSuchElementException("Account not found")

            call.respond(ApiResponse.Success(account))
        }

        post {
            val userId = call.userIdOrThrow()
            val request = call.receive<CreateAccountRequest>()

            log.withContext(
                "userId" to userId,
                "endpoint" to "POST /accounts",
                "accountName" to request.name
            ).info { "Create account request received" }

            val account: AccountDto = accountService.createAccount(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(account))
        }

        put("/{id}") {
            val accountId = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            val request = call.receive<UpdateAccountRequest>()

            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "PUT /accounts/{id}",
                "accountName" to request.name
            ).info { "Update account request received" }

            val updatedAccount: AccountDto =
                accountService.updateAccount(userId, accountId, request)
            call.respond(ApiResponse.Success(updatedAccount))
        }

        delete("/{id}") {
            val accountId = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "DELETE /accounts/{id}"
            ).info { "Delete account request received" }

            val deleted: Boolean = accountService.deleteAccount(userId, accountId)
            if (!deleted) throw NoSuchElementException("Account not found")

            call.respond(ApiResponse.Success("Account deleted successfully"))
        }
    }
}