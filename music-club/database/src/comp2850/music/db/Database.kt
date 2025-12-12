// Database-related code
// (See also Tables.kt & Entities.kt)

package comp2850.music.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

const val DATABASE_URL = "jdbc:sqlite:file:music.db"
const val TEST_DB_URL = "jdbc:sqlite:file:test.db"

fun connectToDatabase() {
    Database.connect(DATABASE_URL)
}

fun createTables() {
    transaction {
        addLogger(StdOutSqlLogger)

        // Create tables

        SchemaUtils.drop(Artists, Albums)
        SchemaUtils.create(Artists, Albums)

        // Create some artists

        fun wiki(page: String) = "https://en.wikipedia.org/wiki/$page"

        val bbt = Artists.insert {
            it[name] = "Big Big Train"
            it[info] = wiki("Big_Big_Train")
        } get Artists.id

        val kateBush = Artists.insert {
            it[name] = "Bush, Kate"
            it[isSolo] = true
            it[info] = wiki("Kate_Bush")
        } get Artists.id

        val d97 = Artists.insert { it[name] = "District 97" } get Artists.id

        val birdsong = Artists.insert { it[name] = "Exploring Birdsong" } get Artists.id

        val genesis = Artists.insert { it[name] = "Genesis" } get Artists.id

        val bethOrton = Artists.insert {
            it[name] = "Orton, Beth"
            it[isSolo] = true
        } get Artists.id

        val steelyDan = Artists.insert {
            it[name] = "Steely Dan"
            it[info] = wiki("Steely_Dan")
        } get Artists.id

        val stevenWilson = Artists.insert {
            it[name] = "Wilson, Steven"
            it[isSolo] = true
            it[info] = wiki("Steven_Wilson")
        } get Artists.id

        // Create some albums

        fun yt(id: String) = "https://www.youtube.com/embed/videoseries?list=$id"

        Albums.insert {
            it[title] = "The Underfall Yard"
            it[artist] = bbt
            it[year] = 2009
            it[youtube] = yt("OLAK5uy_lqFlX_sG6mdiGu1Ky-UFNJz4xcLQ_Qv78")
        }

        Albums.insert {
            it[title] = "Grand Tour"
            it[artist] = bbt
            it[year] = 2019
            it[youtube] = yt("OLAK5uy_kwIU2ctzl4IUVQUy4TXHHYc7y7ZliOTIg")
        }

        Albums.insert {
            it[title] = "Hounds Of Love"
            it[artist] = kateBush
            it[year] = 1985
            it[youtube] = yt("OLAK5uy_myrEBs-bnmBCDOAk1wn5nPvJ2gs88dlE8")
        }

        Albums.insert {
            it[title] = "Stay For The Ending"
            it[artist] = d97
            it[year] = 2023
            it[youtube] = yt("OLAK5uy_kUHAVfYbF0qHXmsVTuRDGcbfmeRczhxNk")
        }

        Albums.insert {
            it[title] = "Trouble With Machines"
            it[artist] = d97
            it[year] = 2012
            it[youtube] = yt("OLAK5uy_kOxN7hQjFMLt6qlhzZBmCJ9HePfECqbSE")
        }

        Albums.insert {
            it[title] = "Duke"
            it[artist] = genesis
            it[year] = 1980
            it[youtube] = yt("OLAK5uy_nGrs32mty30on3l7GuWKZDsPoL2zes5xQ")
        }

        Albums.insert {
            it[title] = "Selling England By The Pound"
            it[artist] = genesis
            it[year] = 1973
        }

        Albums.insert {
            it[title] = "Aja"
            it[artist] = steelyDan
            it[year] = 1977
            it[youtube] = yt("OLAK5uy_nNSt2pxzqur9OlUok2h9mJDnHQ1YqFA-8")
        }

        Albums.insert {
            it[title] = "The Thing With Feathers"
            it[artist] = birdsong
            it[year] = 2019
        }

        Albums.insert {
            it[title] = "The Raven That Refused To Sing"
            it[artist] = stevenWilson
            it[year] = 2013
            it[youtube] = yt("OLAK5uy_minQ5ExLHbNcf5SgPBbPt0rCgNh7eMEVU")
        }

        Albums.insert {
            it[title] = "The Future Bites"
            it[artist] = stevenWilson
            it[year] = 2021
            it[youtube] = yt("OLAK5uy_miw6mx8Z550gm4p43o1PB4l3IgjaAZM6g")
        }

        Albums.insert {
            it[title] = "The Harmony Codex"
            it[artist] = stevenWilson
            it[year] = 2023
            it[youtube] = yt("OLAK5uy_ng3Ruh6KTb_yeqdsM2rOX-RQTj4pNwLyo")
        }

        Albums.insert {
            it[title] = "Central Reservation"
            it[artist] = bethOrton
            it[year] = 1999
            it[youtube] = yt("OLAK5uy_k0EHZkg24DrZ82JMmrhZigTxLm60Wp3cU")
        }
    }
}

fun createDatabase() {
    connectToDatabase()
    createTables()
}

fun connectToTestDatabase() {
    Database.connect(TEST_DB_URL)
}

fun createTestTables() {
    transaction {
        SchemaUtils.drop(Artists, Albums)
        SchemaUtils.create(Artists, Albums)

        val artist1 = Artists.insert {
            it[name] = "A Band"
        } get Artists.id

        val artist2 = Artists.insert {
            it[name] = "Doe, John"
            it[isSolo] = true
        } get Artists.id

        Albums.insert {
            it[title] = "An Album"
            it[artist] = artist1
            it[year] = 2025
        }

        Albums.insert {
            it[title] = "First Album"
            it[artist] = artist2
            it[year] = 2019
        }

        Albums.insert {
            it[title] = "Second Album"
            it[artist] = artist2
            it[year] = 2023
        }
    }
}

fun createTestDatabase() {
    connectToTestDatabase()
    createTestTables()
}
