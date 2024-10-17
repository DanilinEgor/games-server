package org.kuxurum.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.kuxurum.models.orderStorage

fun Route.orderRouting() {
    get("/order") {
        if (orderStorage.isNotEmpty()) {
            call.respond(orderStorage)
        }
    }
    get("/order/{id?}") {
        val id = call.parameters["id"] ?: return@get call.respondText(
            "No id parameter found",
            status = HttpStatusCode.BadRequest
        )

        orderStorage.find { it.number == id }?.let {
            call.respond(it)
        } ?: call.respondText(
            "No order found for id=$id",
            status = HttpStatusCode.NotFound
        )
    }
    get("/order/{id?}/total") {
        val id = call.parameters["id"] ?: return@get call.respondText(
            "No id parameter found",
            status = HttpStatusCode.BadRequest
        )

        orderStorage.find { it.number == id }?.let {
            val total = it.items.sumOf { it.amount * it.price }
            call.respond(total)
        } ?: call.respondText(
            "No order found for id=$id",
            status = HttpStatusCode.NotFound
        )
    }
}