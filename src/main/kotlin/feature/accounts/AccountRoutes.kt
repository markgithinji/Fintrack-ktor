package feature.transaction


import com.fintrack.core.ApiResponse
import com.fintrack.core.logger
import com.fintrack.core.userIdOrThrow
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.data.model.CreateAccountRequest
import com.fintrack.feature.accounts.data.model.UpdateAccountRequest
import com.fintrack.feature.accounts.domain.AccountService
import core.ValidationException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
fun Route.accountsRoutes(accountService: AccountService) {
    val log = logger()

    route("/accounts") {

        get {
            val userId = call.userIdOrThrow()
            log.withContext("userId" to userId, "endpoint" to "GET /accounts")
                .info("Request received")

            val accounts = accountService.getAllAccounts(userId)
            call.respond(ApiResponse.Success(accounts))
        }

        get("/{id}") {
            val accountId = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            log.withContext("userId" to userId, "accountId" to accountId, "endpoint" to "GET /accounts/{id}")
                .info("Request received")

            val account = accountService.getAccount(userId, accountId)
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
            ).info("Request received")

            val account = accountService.createAccount(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(account))
        }

        put("/{id}") {
            val accountId = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            val request = call.receive<UpdateAccountRequest>()

            log.withContext(
                "userId" to userId,
                "accountId" to accountId,
                "endpoint" to "PUT /accounts/{id}",
                "accountName" to request.name
            ).info("Request received")

            val updatedAccount = accountService.updateAccount(userId, accountId, request)
            call.respond(ApiResponse.Success(updatedAccount))
        }

        delete("/{id}") {
            val accountId = call.parameters["id"]?.toIntOrNull()
                ?: throw ValidationException("Invalid account ID")

            val userId = call.userIdOrThrow()
            log.withContext("userId" to userId, "accountId" to accountId, "endpoint" to "DELETE /accounts/{id}")
                .info("Request received")

            val deleted = accountService.deleteAccount(userId, accountId)
            if (!deleted) throw NoSuchElementException("Account not found")

            call.respond(ApiResponse.Success("Account deleted successfully"))
        }
    }
}