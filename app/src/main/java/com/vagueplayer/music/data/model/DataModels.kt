package com.vagueplayer.music.data.model

import android.net.Uri
import java.util.UUID

/**
 * VaguePlayer 统一数据模型 (Unified Data Models)
 * 
 * 合并自: Song.kt, Album.kt, Artist.kt, Playlist.kt, MusicFolder.kt, PlayRecord.kt
 */

// =============================================================================
// 1. 歌曲 (Song)
// =============================================================================

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long,
    val size: Long,
    val path: String = "" // [NEW] For Import/Export matching
)

// =============================================================================
// 2. 专辑 (Album)
// =============================================================================

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val numberOfSongs: Int
)

// =============================================================================
// 3. 歌手 (Artist)
// =============================================================================

data class Artist(
    val id: Long,
    val name: String,
    val numberOfAlbums: Int,
    val numberOfTracks: Int
)

// =============================================================================
// 4. 歌单 (Playlist)
// =============================================================================

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songs: MutableList<Song> = mutableListOf(),
    val dateCreated: Long = System.currentTimeMillis()
)

// =============================================================================
// 5. 音乐文件夹 (Music Folder)
// =============================================================================

data class MusicFolder(
    val uri: Uri,
    val displayName: String,
    val fullPath: String = ""
)

// =============================================================================
// 6. 播放记录 (Play Record)
// =============================================================================

data class PlayRecord(
    val songId: Long,
    val playedAt: Long,
    val playDuration: Long,
    val completed: Boolean
)
