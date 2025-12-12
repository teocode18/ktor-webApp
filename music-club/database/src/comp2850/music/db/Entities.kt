// Mapping of entity classes onto DB tables for Music Club app

package comp2850.music.db

import org.jetbrains.exposed.dao.UIntEntity
import org.jetbrains.exposed.dao.UIntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Artist(id: EntityID<UInt>): UIntEntity(id) {
    companion object: UIntEntityClass<Artist>(Artists)

    var name by Artists.name
    var isSolo by Artists.isSolo
    var info by Artists.info
    val albums by Album referrersOn Albums.artist

    val properName: String get() {
        if (isSolo) {
            val parts = name.split(",").map { it.trim() }
            return "${parts[1]} ${parts[0]}"
        }
        return name
    }

    override fun toString() = name
}

class Album(id: EntityID<UInt>): UIntEntity(id) {
    companion object: UIntEntityClass<Album>(Albums)

    var artist by Artist referencedOn Albums.artist
    var title by Albums.title
    var year by Albums.year
    var youtube by Albums.youtube

    override fun toString() = title
}
