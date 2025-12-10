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
        "dieOptions" to dieOptions
    ))
}

private suspend fun ApplicationCall.handleDiceRoll() {
    val (num, die) = extractParameters(receiveParameters())
    val results = diceRoll(num, die)
    respondTemplate("results.peb", model = mapOf(
        "num" to num,
        "die" to die,
        "results" to results,
        "total" to results.sum(),
    ))
}

private fun extractParameters(params: Parameters) = Pair(
    params["num"]?.toInt() ?: error("Number of dice not specified"),
    params["die"] ?: error("Die not specified")
)
