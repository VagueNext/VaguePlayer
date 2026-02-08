package com.vagueplayer.music.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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

@Parcelize
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
) : Parcelable

// =============================================================================
// 2. 专辑 (Album)
// =============================================================================

@Parcelize
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val numberOfSongs: Int
) : Parcelable

// =============================================================================
// 3. 歌手 (Artist)
// =============================================================================

@Parcelize
data class Artist(
    val id: Long,
    val name: String,
    val numberOfAlbums: Int,
    val numberOfTracks: Int
) : Parcelable

// =============================================================================
// 4. 歌单 (Playlist)
// =============================================================================

@Parcelize
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songs: MutableList<Song> = mutableListOf(),
    val dateCreated: Long = System.currentTimeMillis()
) : Parcelable

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
    
    // =========================
    // 基础计数
    // =========================
    val playCount: Int = 0,
    val skipCount: Int = 0,             // 跳过次数 (5s ≤ 播放时长 < 30s)
    val completionCount: Int = 0,       // 完播次数 (播放 ≥ 90% 或结束)
    val cutCount: Int = 0,              // 中途切歌次数 (30s ≤ 播放时长 < 60%)
    val totalPlayDuration: Long = 0,    // 总收听时长 (毫秒)
    
    // =========================
    // 交互行为
    // =========================
    val clickCount: Int = 0,            // 主动点击/播放次数
    val addToPlaylistCount: Int = 0,    // 加入歌单次数
    val searchHitCount: Int = 0,        // 搜索命中次数
    val isFavorite: Boolean = false,    // 是否收藏
    
    // =========================
    // 时间维度统计
    // =========================
    // 时间段分布: "MORNING", "NOON", "EVENING", "LATE_NIGHT"
    val timeOfDayStats: Map<String, Int>? = null,
    // 星期分布: "WEEKDAY", "WEEKEND"
    val dayOfWeekStats: Map<String, Int>? = null,
    
    // 最近 5 天播放/跳过时间戳列表（用于计算近期热度）
    val lastFiveDaysPlays: List<Long>? = null,
    val lastFiveDaysSkips: List<Long>? = null,
    
    // 保留最近 20 次播放时间戳（用于时间偏好分析）
    val playbackHistory: List<Long>? = null,
    
    // =========================
    // 上下文与元数据
    // =========================
    val lastPlayedAt: Long = 0,         // 上一次播放时间
    val lastDevice: String? = null,     // 最后播放设备: "HEADPHONES", "SPEAKER", "BLUETOOTH", "BUILT_IN"
    val lastRecommendedAt: Long = 0,    // 最近一次推荐时间（用于冷却期控制）
    
    // =========================
    // 推荐反馈（仅当歌曲因推荐而播放时统计）
    // =========================
    val recommendationPlayCount: Int = 0,       // 推荐后播放次数
    val recommendationSkipCount: Int = 0,       // 推荐后跳过次数
    val recommendationCompleteCount: Int = 0,   // 推荐后完播次数
    
    // =========================
    // 音频特征（预留，需要音频分析库支持）
    // =========================
    val bpm: Int? = null,               // 每分钟节拍数
    val loudness: Float? = null,        // 响度 (dB)
    val language: String? = null        // 歌曲语言: "ZH", "EN", "JA", "KO", "OTHER"
)

// =============================================================================
// 8. 每日推荐状态 (Recommendation State)
// =============================================================================

data class RecommendationState(
    val lastRefreshTime: Long = 0,
    val nextRefreshTime: Long = 0, // Target time for next refresh (e.g., Now + 8 Hours)
    val recommendedSongIds: List<Long> = emptyList(),
    val recommendationReasons: Map<Long, String> = emptyMap() // SongID -> Reason (e.g., "From Hot Pool", "Rediscover")
)
