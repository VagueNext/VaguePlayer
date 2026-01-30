package com.vagueplayer.music.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "songs_meta",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class SongsMetaEntity(
    @PrimaryKey
    val id: Long, // MediaStore ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: String, // Persist as String
    val albumArtUri: String?, // Persist as String?
    val dateAdded: Long,
    val size: Long,
    val path: String
)
