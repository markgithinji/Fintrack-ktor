package feature.transactions


import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.accounts.data.model.AccountDto
import com.fintrack.feature.accounts.domain.AccountService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.accountsRoutes(accountService: AccountService) {
    route("/accounts") {

        get {
            val userId = call.userIdOrThrow()
            val accounts = accountService.getAllAccounts(userId)
            call.respond(ApiResponse.Success(accounts))
        }

        get("/{id}") {
            val accountId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid account id"))

            val userId = call.userIdOrThrow()
            val account = accountService.getAccount(userId, accountId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Account not found"))

            call.respond(ApiResponse.Success(account))
        }

        post {
            val userId = call.userIdOrThrow()
            val request = try { call.receive<AccountDto>() }
            catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid request body"))
            }

            val account = accountService.createAccount(userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse.Success(account))
        }

        put("/{id}") {
            val accountId = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid account id"))

            val userId = call.userIdOrThrow()
            val request = try { call.receive<AccountDto>() }
            catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid request body"))
            }

            val updatedAccount = accountService.updateAccount(userId, accountId, request)
                ?: return@put call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Account not found"))

            call.respond(ApiResponse.Success(updatedAccount))
        }

        delete("/{id}") {
            val accountId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid account id"))

            val userId = call.userIdOrThrow()
            val deleted = accountService.deleteAccount(userId, accountId)
            if (!deleted) return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Account not found"))

            call.respond(ApiResponse.Success("Account deleted successfully"))
        }
    }
}
