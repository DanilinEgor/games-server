package org.kuxurum.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.kuxurum.models.Customer
import org.kuxurum.models.customerStorage

fun Route.customerRouting() {
    route("/customer") {
        get {
            if (customerStorage.isEmpty()) {
                call.respondText("No customers found", status = HttpStatusCode.OK)
            } else {
                call.respond(customerStorage)
            }
        }
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "No id parameter found",
                status = HttpStatusCode.BadRequest
            )

            customerStorage.find { it.id == id }?.let {
                call.respond(it)
            } ?: call.respondText(
                "No customer found for id=$id",
                status = HttpStatusCode.NotFound
            )
        }
        post {
            val customer = call.receive<Customer>()
            customerStorage.add(customer)
            call.respondText("Customer stored successfully", status = HttpStatusCode.Created)
        }
        delete("{id?}") {
            val id = call.parameters["id"] ?: return@delete call.respondText(
                "No id parameter found",
                status = HttpStatusCode.BadRequest
            )

            if (customerStorage.removeIf { it.id == id }) {
                call.respondText("Customer removed successfully", status = HttpStatusCode.Accepted)
            } else {
                call.respondText(
                    "No customer found for id=$id",
                    status = HttpStatusCode.NotFound
                )
            }
        }
    }
}