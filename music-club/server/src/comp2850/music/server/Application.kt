// Application configuration & entry point

package comp2850.music.server

import comp2850.music.db.connectToDatabase
import comp2850.music.db.connectToTestDatabase
import io.ktor.server.application.Application

fun Application.module() {
    connectToDatabase()
    configureTemplates()
    configureRouting()
}

fun Application.testModule() {
    connectToTestDatabase()
    configureTemplates()
    configureRouting()
}

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}
