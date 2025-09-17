package feature.user

import com.fintrack.core.ApiResponse
import com.fintrack.feature.transactions.userIdOrThrow
import com.fintrack.feature.user.data.UserDto
import com.fintrack.feature.user.data.UserRepository
import core.AvailableWeeks
import core.PaginatedTransactionDto
import core.TransactionDto
import core.ValidationException
import core.toDto
import core.toTransaction
import core.validate
import io.ktor.http.*
import io.ktor.server.auth.authenticate
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

fun Route.userRoutes() {
    val userRepo = UserRepository()
    route("/users") {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asInt()
            val user = userRepo.findById(userId)

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            } else {
                call.respond(
                    UserDto(
                        id = user.id.toString(),
                        name = user.username,
                        email = user.username,
                        token = "" // not needed here
                    )
                )
            }
        }
    }
}