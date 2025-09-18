package feature.transactions


import com.fintrack.core.ApiResponse
import com.fintrack.feature.transactions.userIdOrThrow
import core.AccountDto
import core.AccountsRepository
import core.AvailableWeeks
import core.PaginatedTransactionDto
import core.TransactionDto
import core.ValidationException
import core.toDomain
import core.toDto
import core.toTransaction
import core.validate
import io.ktor.http.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.temporal.IsoFields

fun Route.accountsRoutes() {
    val accountsRepository = AccountsRepository()

    route("/accounts") {

        // Get all accounts for current user
        get {
            val userId = call.userIdOrThrow()
            val accounts = accountsRepository.getAllAccounts(userId)
                .map { it.toDto() }
            call.respond(ApiResponse.Success(accounts))
        }

        // Get a single account by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Missing or invalid account id"))

            val userId = call.userIdOrThrow()
            val account = accountsRepository.getAccountById(id)
            if (account == null || account.userId != userId) {
                call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Account not found"))
            } else {
                call.respond(ApiResponse.Success(account.toDto()))
            }
        }

        // Create a new account
        post {
            val userId = call.userIdOrThrow()
            val request = try { call.receive<AccountDto>() }
            catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid request body"))
            }

            val account = accountsRepository.addAccount(request.toDomain(userId))
            call.respond(HttpStatusCode.Created, ApiResponse.Success(account.toDto()))
        }

        // Update an existing account
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Missing or invalid account id"))

            val userId = call.userIdOrThrow()
            val request = try { call.receive<AccountDto>() }
            catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid request body"))
            }

            val updatedAccount = accountsRepository.updateAccount(
                request.toDomain(userId).copy(id = id)
            )
            call.respond(ApiResponse.Success(updatedAccount.toDto()))
        }

        // Delete an account
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Missing or invalid account id"))

            val userId = call.userIdOrThrow()
            val account = accountsRepository.getAccountById(id)
            if (account == null || account.userId != userId) {
                call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Account not found"))
                return@delete
            }

            accountsRepository.deleteAccount(id)
            call.respond(ApiResponse.Success("Account deleted successfully"))
        }
    }
}
