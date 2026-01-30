package com.vagueplayer.music.data.database

import androidx.room.Entity
import androidx.room.Fts4

@Fts4
@Entity(tableName = "lyrics_fts")
data class LyricsFtsEntity(
    val songId: Long,
    val content: String // Complete lyrics text for searching
)
