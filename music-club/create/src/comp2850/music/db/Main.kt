// Creates the Music Club database

package comp2850.music.db

fun main(args: Array<String>) {
    val sqlLog = args.isNotEmpty() && args[0].lowercase() == "--sql"
    println("Creating ${MusicDatabase.URL}...")
    MusicDatabase.create(sqlLog)
}
