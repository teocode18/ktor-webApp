// Set up application routing & request handling

import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.pebble.respondTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") { call.displayForm() }
        post("/roll") { call.handleDiceRoll() }
    }
}

private suspend fun ApplicationCall.displayForm() {
    respondTemplate("form.peb", model = mapOf(
        "sidesOptions" to sidesOptions
    ))
}

private suspend fun ApplicationCall.handleDiceRoll() {
    val (dice, sides) = extractParameters(receiveParameters())
    val results = diceRoll(dice, sides)
    respondTemplate("results.peb", model = mapOf(
        "dice" to dice,
        "sides" to sides,
        "results" to results,
        "total" to results.sum(),
    ))
}

private fun extractParameters(params: Parameters) = Pair(
    params["dice"]?.toInt() ?: error("Number of dice not specified"),
    params["sides"]?.toInt() ?: error("Number of sides not specified")
)
