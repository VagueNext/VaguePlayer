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

// =============================================================================
// 7. 歌曲统计信息 (Song Statistics) - For Recommendation Engine
// =============================================================================

data class SongStatistics(
    val songId: Long,
    // 基础计数
    val playCount: Int = 0,
    val skipCount: Int = 0,     // 播放 < 30s 视为跳过 (可配置)
    val completionCount: Int = 0, // 播放 > 90% 视为只有
    
    // 交互行为
    val clickCount: Int = 0,    // 主动点击/播放
    val addToPlaylistCount: Int = 0,
    val searchHitCount: Int = 0,
    
    // 时间维度
    val lastPlayedAt: Long = 0,
    val playbackHistory: List<Long> = emptyList(), // 保留最近 20 次播放时间戳，用于分析时间段偏好
    
    // 上下文
    val lastDevice: String? = null // "HEADPHONES", "SPEAKER", "BLUETOOTH"
)

// =============================================================================
// 8. 每日推荐状态 (Recommendation State)
// =============================================================================

data class RecommendationState(
    val lastRefreshTime: Long = 0,
    val recommendedSongIds: List<Long> = emptyList(),
    val recommendationReasons: Map<Long, String> = emptyMap() // SongID -> Reason (e.g., "From Hot Pool", "Rediscover")
)
