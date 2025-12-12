// Routing and application logic for Music Club app
// (see also the templates in src/resources/templates)

package comp2850.music.server

import comp2850.music.db.Album
import comp2850.music.db.Albums
import comp2850.music.db.Artist
import comp2850.music.db.Artists
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.pebble.respondTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Application.configureRouting() {
    routing {
        get("/") {
            newSuspendedTransaction {
                call.respondTemplate("index.peb", mapOf(
                    "albums" to Album.count(),
                    "artists" to Artist.count(),
                ))
            }
        }

        post("/search") {
            newSuspendedTransaction {
                val formParams = call.receiveParameters()
                val searchTerm = formParams["search_term"] ?: ""
                val results = Album.find { Albums.title like "%$searchTerm%" }
                call.respondTemplate("search.peb", mapOf(
                    "searchTerm" to searchTerm,
                    "results" to results,
                ))
            }
        }

        get("/artists") {
            newSuspendedTransaction {
                val artists = Artist.all().sortedBy { it.name }.toList()
                call.respondTemplate("artists.peb", mapOf("artists" to artists))
            }
        }

        get("/artists/{id}") {
            newSuspendedTransaction {
                val result = runCatching {
                    call.parameters["id"]?.let {
                        Artist.findById(it.toUInt())
                    }
                }

                val artist = result.getOrNull()

                when (artist) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> {
                        val albums = artist.albums.sortedBy { it.year }.toList()
                        call.respondTemplate("artist.peb", mapOf(
                            "name" to artist.name,
                            "solo" to artist.isSolo,
                            "info" to (artist.info ?: ""),
                            "albums" to albums,
                        ))
                    }
                }
            }
        }

        get("/albums") {
            newSuspendedTransaction {
                val comparator = compareBy<Album> { it.artist.name }.thenBy { it.year }
                val albums = Album.all().sortedWith(comparator).toList()
                call.respondTemplate("albums.peb", mapOf("albums" to albums))
            }
        }

        get("/albums/{id}") {
            newSuspendedTransaction {
                val result = runCatching {
                    call.parameters["id"]?.let {
                        Album.findById(it.toUInt())
                    }
                }

                val album = result.getOrNull()

                when (album) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respondTemplate("album.peb", mapOf(
                        "artist" to album.artist,
                        "title" to album.title,
                        "year" to album.year,
                        "youtube" to (album.youtube ?: "")
                    ))
                }
            }
        }
    }
}
