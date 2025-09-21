package feature.transactions


import com.fintrack.core.ApiResponse
import com.fintrack.core.userIdOrThrow
import com.fintrack.feature.summary.data.repository.StatisticsRepository
import feature.accounts.data.AccountDto
import feature.accounts.data.AccountsRepository
import feature.accounts.data.toDomain
import feature.accounts.data.toDto
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.accountsRoutes(
) {
    val accountsRepository = AccountsRepository()
    val statisticsRepository = StatisticsRepository()

    route("/accounts") {

        // Get all accounts for current user
        get {
            val userId = call.userIdOrThrow()
            val accounts = accountsRepository.getAllAccounts(userId)
                .map { account ->
                    val aggregates = statisticsRepository.getAccountAggregates(userId, account.id)
                    account.toDto(
                        income = aggregates.income,
                        expense = aggregates.expense,
                        balance = aggregates.balance
                    )
                }
            call.respond(ApiResponse.Success(accounts))
        }

        // Get a single account by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Missing or invalid account id")
                )

            val userId = call.userIdOrThrow()
            val account = accountsRepository.getAccountById(id)
            if (account == null || account.userId != userId) {
                call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Account not found"))
            } else {
                val aggregates = statisticsRepository.getAccountAggregates(userId, account.id)
                call.respond(
                    ApiResponse.Success(
                        account.toDto(
                            income = aggregates.income,
                            expense = aggregates.expense,
                            balance = aggregates.balance
                        )
                    )
                )
            }
        }

        // Create a new account
        post {
            val userId = call.userIdOrThrow()
            val request = try {
                call.receive<AccountDto>()
            } catch (e: Exception) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Invalid request body")
                )
            }

            val account = accountsRepository.addAccount(request.toDomain(userId))
            call.respond(
                HttpStatusCode.Created,
                ApiResponse.Success(account.toDto()) // no aggregates yet (brand new account)
            )
        }

        // Update an existing account
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Missing or invalid account id")
                )

            val userId = call.userIdOrThrow()
            val request = try {
                call.receive<AccountDto>()
            } catch (e: Exception) {
                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Invalid request body")
                )
            }

            val updatedAccount = accountsRepository.updateAccount(
                request.toDomain(userId).copy(id = id)
            )

            val aggregates = statisticsRepository.getAccountAggregates(userId, updatedAccount.id)
            call.respond(
                ApiResponse.Success(
                    updatedAccount.toDto(
                        income = aggregates.income,
                        expense = aggregates.expense,
                        balance = aggregates.balance
                    )
                )
            )
        }

        // Delete an account
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Missing or invalid account id")
                )

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
