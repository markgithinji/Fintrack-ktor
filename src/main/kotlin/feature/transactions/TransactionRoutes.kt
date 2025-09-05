package com.fintrack.feature.transactions

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transactionRoutes() {
    route("/transactions") {
        // GET /transactions → list all
        get {
            call.respond(TransactionRepository.getAll())
        }

        // GET /transactions/{id} → get by id
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            val transaction = TransactionRepository.getById(id)
            if (transaction == null) {
                call.respond(HttpStatusCode.NotFound, "Transaction not found")
            } else {
                call.respond(transaction)
            }
        }

        // POST /transactions → add new
        post {
            val transaction = call.receive<Transaction>()
            val saved = TransactionRepository.add(transaction)
            call.respond(HttpStatusCode.Created, saved)
        }

        // PUT /transactions/{id} → update
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }
            val transaction = call.receive<Transaction>()
            val updated = TransactionRepository.update(id, transaction)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, "Transaction not found")
            } else {
                call.respond(updated)
            }
        }

        // DELETE /transactions/{id} → remove
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            val deleted = TransactionRepository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Transaction not found")
            }
        }
    }
}

