// Database schema for the Music Club app

package comp2850.music.db

import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

const val MAX_VARCHAR_LENGTH = 256

object Artists: UIntIdTable() {
    val name = varchar("name", MAX_VARCHAR_LENGTH)
    val isSolo = bool("is_solo").default(false)
    val info = varchar("info_url", MAX_VARCHAR_LENGTH).nullable()
}

object Albums: UIntIdTable() {
    val artist = reference("artist_id", Artists, ReferenceOption.CASCADE)
    val title = varchar("title", MAX_VARCHAR_LENGTH)
    val year = integer("year")
    val youtube = varchar("youtube_url", MAX_VARCHAR_LENGTH).nullable()
}
