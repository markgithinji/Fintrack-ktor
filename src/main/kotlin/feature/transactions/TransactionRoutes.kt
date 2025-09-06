package com.fintrack.feature.transactions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transactionRoutes() {
    val repo = TransactionRepository()

    route("/transactions") {
        // GET /transactions → list all
        get {
            call.respond(repo.getAll())
        }

        // GET /transactions/{id} → single
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

            val transaction = repo.getById(id)
            if (transaction != null) {
                call.respond(transaction)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transaction not found"))
            }
        }

        // POST /transactions → create
        post {
            val transaction = call.receive<Transaction>()
            val saved = repo.add(transaction)
            call.respond(HttpStatusCode.Created, saved)
        }

        // PUT /transactions/{id} → full update
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

            val body = call.receive<Transaction>()
            val success = repo.update(id, body)

            if (success) {
                call.respond(HttpStatusCode.OK, repo.getById(id)!!)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transaction not found"))
            }
        }

        // DELETE /transactions/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

            val success = repo.delete(id)
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Transaction deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transaction not found"))
            }
        }
    }
}
