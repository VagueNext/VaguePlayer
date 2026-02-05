package com.vagueplayer.music.data.engine

import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.data.model.SongStatistics
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class RecommendationEngine {

    companion object {
        private const val MIN_RECOMMENDATION_COUNT = 20
        private const val MAX_RECOMMENDATION_COUNT = 40
        private const val RATIO = 0.15
    }

    data class RecommendationResult(
        val songs: List<Song>,
        val reasons: Map<Long, String> // SongId -> Reason
    )

    fun generateDailyRecommendations(
        allSongs: List<Song>,
        statsMap: Map<Long, SongStatistics>
    ): RecommendationResult {
        if (allSongs.isEmpty()) {
            return RecommendationResult(emptyList(), emptyMap())
        }

        // 1. Calculate target count
        val targetCount = (allSongs.size * RATIO).toInt().coerceIn(MIN_RECOMMENDATION_COUNT, MAX_RECOMMENDATION_COUNT)
        val finalCount = min(targetCount, allSongs.size) // Ensure we don't ask for more than available

        // 2. Prepare pools
        val hotPool = mutableListOf<Song>()
        val rediscoverPool = mutableListOf<Song>()
        val timePool = mutableListOf<Song>()
        val explorePool = mutableListOf<Song>()
        val restPool = allSongs.toMutableList() // Songs not yet selected, initially all

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val sevenDaysMs = 7 * oneDayMs
        val thirtyDaysMs = 30 * oneDayMs

        // Helper to check stats
        fun getStats(songId: Long) = statsMap[songId] ?: SongStatistics(songId)

        // Categorize songs
        allSongs.forEach { song ->
            val stat = getStats(song.id)
            val daysSinceLastPlay = if (stat.lastPlayedAt > 0) (now - stat.lastPlayedAt) else Long.MAX_VALUE

            // Time preference check (simple hour matching)
            val matchesTimeContext = if (stat.playbackHistory.isNotEmpty()) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                stat.playbackHistory.any { timestamp ->
                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                    val historyHour = cal.get(Calendar.HOUR_OF_DAY)
                    kotlin.math.abs(currentHour - historyHour) <= 2 // +/- 2 hours window
                }
            } else false

            // Logic for pools
            when {
                // Hot Pool: Frequent & High Completion
                stat.playCount > 5 && (stat.completionCount.toFloat() / stat.playCount.coerceAtLeast(1) > 0.7f) -> {
                    hotPool.add(song)
                }
                // Rediscover: Played before (activity exists) but not recently (7-30 days ago)
                stat.playCount > 0 && daysSinceLastPlay in sevenDaysMs..thirtyDaysMs -> {
                    rediscoverPool.add(song)
                }
                // Time Preference: Matches current time context
                matchesTimeContext -> {
                    timePool.add(song)
                }
                // Explore: Low play count or never played
                stat.playCount < 3 -> {
                    explorePool.add(song)
                }
            }
        }

        // 3. Selection with distribution
        // Target: Hot 40%, Rediscover 30%, Time 20%, Explore 10%
        val countHot = (finalCount * 0.40).toInt()
        val countRediscover = (finalCount * 0.30).toInt()
        val countTime = (finalCount * 0.20).toInt()
        val countExplore = (finalCount * 0.10).toInt()
        // Remainder fills any gaps
        
        val selectedSongs = mutableSetOf<Song>()
        val reasons = mutableMapOf<Long, String>()

        fun selectFromPool(pool: MutableList<Song>, count: Int, reason: String) {
            val shuffled = pool.shuffled()
            var added = 0
            for (song in shuffled) {
                if (added >= count) break
                if (selectedSongs.add(song)) {
                    reasons[song.id] = reason
                    restPool.remove(song)
                    added++
                }
            }
        }

        selectFromPool(hotPool, countHot, "常听的歌")
        selectFromPool(rediscoverPool, countRediscover, "很久未听")
        selectFromPool(timePool, countTime, "此时此刻")
        selectFromPool(explorePool, countExplore, "猜你喜欢")

        // Fill remaining quota from Rest Pool (Random shuffle of remaining)
        val remainingNeeded = finalCount - selectedSongs.size
        if (remainingNeeded > 0) {
            val remainingShuffled = restPool.filter { !selectedSongs.contains(it) }.shuffled()
             for (song in remainingShuffled) {
                if (selectedSongs.size >= finalCount) break
                if (selectedSongs.add(song)) {
                    // Determine a generic reason based on stats if possible, or default
                    val stat = getStats(song.id)
                    val reason = when {
                        stat.playCount > 10 -> "我的最爱"
                        stat.playCount == 0 -> "不仅好听"
                        else -> "随心听听"
                    }
                    reasons[song.id] = reason
                }
            }
        }
        
        // If still not enough (total songs < MIN), we just take what we have (should be all)

        return RecommendationResult(selectedSongs.toList().shuffled(), reasons)
    }
}
