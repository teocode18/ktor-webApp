// Example of using Exposed's DSL for executing SQL queries
// (See server for examples of using the DAO approach)

package comp2850.music.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    connectToDatabase()

    transaction {
        addLogger(StdOutSqlLogger)

        val artistCount = Artists.selectAll().count()
        val albumCount = Albums.selectAll().count()
        val albums = (Albums innerJoin Artists).select(Artists.name, Albums.title)

        println("Artists found: $artistCount")
        println("Albums found: $albumCount")

        albums.forEach {
            println("${it[Artists.name]} - ${it[Albums.title]}")
        }
    }
}
