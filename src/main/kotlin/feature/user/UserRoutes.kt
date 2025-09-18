package feature.user

import com.fintrack.feature.user.data.UserDto
import com.fintrack.feature.user.data.UserRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
                        name = user.username,
                        email = user.username
                    )
                )
            }
        }
    }
}