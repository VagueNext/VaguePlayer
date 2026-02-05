package com.vagueplayer.music.data.engine

import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.data.model.SongStatistics
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

/**
 * 升级版推荐引擎 - 基于规则的多维度评分系统
 * 
 * 核心特征：
 * - 冷却期控制（3天内推荐过的不再推荐）
 * - 多维度评分（完播率、跳过率、时间契合度、设备契合度等）
 * - 多样性控制（同歌手/专辑限制）
 * - 新歌保护（7天内下载的获得权重提升）
 */
class RecommendationEngine {

    companion object {
        private const val MIN_RECOMMENDATION_COUNT = 20
        private const val MAX_RECOMMENDATION_COUNT = 40
        private const val RATIO = 0.15
        
        // Cooldown & Time Windows
        private const val RECOMMENDATION_COOLDOWN_MS = 3 * 24 * 60 * 60 * 1000L  // 3 days
        private const val NEW_SONG_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L          // 7 days
        private const val FIVE_DAYS_MS = 5 * 24 * 60 * 60 * 1000L
        
        // Diversity Limits
        private const val MAX_SAME_ARTIST = 4
        private const val MAX_SAME_ALBUM = 3
        
        // Cut Rate Threshold (中途切歌率阈值)
        private const val HIGH_CUT_RATE_THRESHOLD = 0.4f
    }

    data class RecommendationResult(
        val songs: List<Song>,
        val reasons: Map<Long, String>
    )

    fun generateDailyRecommendations(
        allSongs: List<Song>,
        statsMap: Map<Long, SongStatistics>,
        currentDevice: String? = null
    ): RecommendationResult {
        if (allSongs.isEmpty()) {
            return RecommendationResult(emptyList(), emptyMap())
        }

        val now = System.currentTimeMillis()
        val targetCount = (allSongs.size * RATIO).toInt()
            .coerceIn(MIN_RECOMMENDATION_COUNT, MAX_RECOMMENDATION_COUNT)
            .coerceAtMost(allSongs.size)

        // Step 1: Filter out songs in cooldown period
        val eligibleSongs = allSongs.filter { song ->
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            val timeSinceRecommended = now - stats.lastRecommendedAt
            timeSinceRecommended >= RECOMMENDATION_COOLDOWN_MS || stats.lastRecommendedAt == 0L
        }

        if (eligibleSongs.isEmpty()) {
            return RecommendationResult(emptyList(), emptyMap())
        }

        // Step 2: Calculate scores for all eligible songs
        val scoredSongs = eligibleSongs.map { song ->
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            val score = calculateScore(song, stats, now, currentDevice)
            ScoredSong(song, stats, score)
        }

        // Step 3: Sort by score (descending)
        val sortedByScore = scoredSongs.sortedByDescending { it.score }

        // Step 4: Apply diversity filters and select top songs
        val selected = applyDiversityFilters(sortedByScore, targetCount)

        // Step 5: Generate reasons
        val reasons = selected.associate { it.song.id to generateReason(it.song, it.stats, now) }

        return RecommendationResult(
            songs = selected.map { it.song }.shuffled(), // Shuffle final list for variety
            reasons = reasons
        )
    }

    private data class ScoredSong(
        val song: Song,
        val stats: SongStatistics,
        val score: Float
    )

    private fun calculateScore(
        song: Song,
        stats: SongStatistics,
        now: Long,
        currentDevice: String?
    ): Float {
        var score = 0f

        // === 基础权重 ===
        // 1. 完播率 (0-30分)
        val completionRate = if (stats.playCount > 0) {
            stats.completionCount.toFloat() / stats.playCount
        } else 0f
        score += completionRate * 30f

        // 2. 跳过率惩罚 (-0到-20分)
        val skipRate = if (stats.playCount > 0) {
            stats.skipCount.toFloat() / stats.playCount
        } else 0f
        score -= skipRate * 20f

        // 3. 中途切歌率惩罚 (-0到-15分)
        val cutRate = if (stats.playCount > 0) {
            stats.cutCount.toFloat() / stats.playCount
        } else 0f
        if (cutRate > HIGH_CUT_RATE_THRESHOLD) {
            score -= 15f // 严重惩罚高切歌率
        }

        // 4. 播放次数加成 (0-10分, 适度热度)
        val playBonus = when {
            stats.playCount > 20 -> 10f
            stats.playCount > 10 -> 7f
            stats.playCount > 5 -> 5f
            stats.playCount > 0 -> 3f
            else -> 0f
        }
        score += playBonus

        // === 时间维度 ===
        // 5. 当前时间段契合度 (0-15分)
        val timeScore = calculateTimeScore(stats, now)
        score += timeScore

        // 6. 星期契合度 (0-10分)
        val dayScore = calculateDayScore(stats, now)
        score += dayScore

        // === 用户行为 ===
        // 7. 主动点击/收藏加成 (0-10分)
        if (stats.isFavorite) {
            score += 10f
        } else if (stats.clickCount > 0) {
            score += (stats.clickCount.coerceAtMost(5) * 2f)
        }

        // 8. 最近5天活跃度 (0-10分)
        val recentPlays = (stats.lastFiveDaysPlays ?: emptyList()).count { now - it <= FIVE_DAYS_MS }
        val recentSkips = (stats.lastFiveDaysSkips ?: emptyList()).count { now - it <= FIVE_DAYS_MS }
        val recentScore = if (recentPlays > 0) {
            val recentCompletionRate = (recentPlays - recentSkips).toFloat() / recentPlays
            recentCompletionRate * 10f
        } else 0f
        score += recentScore

        // === 设备契合度 ===
        // 9. 设备匹配 (0-5分)
        if (currentDevice != null && stats.lastDevice == currentDevice) {
            score += 5f
        }

        // === 新歌保护 ===
        // 10. 新下载歌曲权重提升 (0-15分)
        val timeSinceAdded = now - song.dateAdded
        if (timeSinceAdded <= NEW_SONG_WINDOW_MS && stats.playCount == 0) {
            score += 15f // 新歌且未播放，高权重
        } else if (timeSinceAdded <= NEW_SONG_WINDOW_MS) {
            score += 8f // 新歌但已播放过
        }

        // === 推荐反馈 ===
        // 11. 推荐效果反馈 (0-8分 or -0到-8分)
        if (stats.recommendationPlayCount > 0) {
            val recCompletionRate = stats.recommendationCompleteCount.toFloat() / stats.recommendationPlayCount
            val recSkipRate = stats.recommendationSkipCount.toFloat() / stats.recommendationPlayCount
            score += (recCompletionRate * 8f) - (recSkipRate * 8f)
        }

        return score.coerceAtLeast(0f)
    }

    private fun calculateTimeScore(stats: SongStatistics, now: Long): Float {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val currentPeriod = when (hour) {
            in 5..10 -> "MORNING"
            in 11..13 -> "NOON"
            in 14..18 -> "AFTERNOON"
            in 19..23 -> "NIGHT"
            else -> "LATE_NIGHT"
        }

        val timeStats = stats.timeOfDayStats ?: return 0f
        val totalTimePlays = timeStats.values.sum()
        if (totalTimePlays == 0) return 0f

        val currentPeriodPlays = timeStats[currentPeriod] ?: 0
        val periodRatio = currentPeriodPlays.toFloat() / totalTimePlays

        return periodRatio * 15f
    }

    private fun calculateDayScore(stats: SongStatistics, now: Long): Float {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val currentDayType = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            "WEEKEND"
        } else {
            "WEEKDAY"
        }

        val dayStats = stats.dayOfWeekStats ?: return 0f
        val totalDayPlays = dayStats.values.sum()
        if (totalDayPlays == 0) return 0f

        val currentDayPlays = dayStats[currentDayType] ?: 0
        val dayRatio = currentDayPlays.toFloat() / totalDayPlays

        return dayRatio * 10f
    }

    private fun applyDiversityFilters(
        sortedSongs: List<ScoredSong>,
        targetCount: Int
    ): List<ScoredSong> {
        val selected = mutableListOf<ScoredSong>()
        val artistCount = mutableMapOf<String, Int>()
        val albumCount = mutableMapOf<String, Int>()

        for (scoredSong in sortedSongs) {
            if (selected.size >= targetCount) break

            val artist = scoredSong.song.artist
            val album = scoredSong.song.album
            val currentArtistCount = artistCount[artist] ?: 0
            val currentAlbumCount = albumCount[album] ?: 0

            // Apply diversity rules
            if (currentArtistCount >= MAX_SAME_ARTIST) continue
            if (currentAlbumCount >= MAX_SAME_ALBUM) continue

            selected.add(scoredSong)
            artistCount[artist] = currentArtistCount + 1
            albumCount[album] = currentAlbumCount + 1
        }

        return selected
    }

    private fun generateReason(song: Song, stats: SongStatistics, now: Long): String {
        val timeSinceAdded = now - song.dateAdded

        return when {
            timeSinceAdded <= NEW_SONG_WINDOW_MS && stats.playCount == 0 -> "新歌推荐"
            stats.isFavorite -> "你的最爱"
            stats.playCount > 20 -> "热门单曲"
            stats.completionCount > 0 && stats.completionCount.toFloat() / stats.playCount > 0.8f -> "常听完整"
            calculateTimeScore(stats, now) > 10f -> "契合此刻"
            stats.playCount == 0 -> "猜你喜欢"
            now - stats.lastPlayedAt > 7 * 24 * 60 * 60 * 1000L -> "好久不见"
            else -> "为你推荐"
        }
    }
}
