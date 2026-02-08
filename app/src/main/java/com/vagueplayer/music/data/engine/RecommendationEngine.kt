package com.vagueplayer.music.data.engine

import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.data.model.SongStatistics
import kotlin.math.max
import kotlin.math.min

/**
 * 升级版推荐引擎 (v2.0) - 基于配额池的推荐系统
 * 
 * 核心规则：
 * 1. 总数量: 15% (Min 20, Max 40)
 * 2. 四大推荐池:
 *    - 热歌池 (40%): 播放次数最多
 *    - 延伸池 (30%): 基于热歌的歌手关联
 *    - 回溯池 (20%): >30天未播放
 *    - 挖掘池 (10%): 冷门/新歌
 */
class RecommendationEngine {

    companion object {
        private const val MIN_RECOMMENDATION_COUNT = 20
        private const val MAX_RECOMMENDATION_COUNT = 40
        private const val RATIO = 0.15
        
        // Cooldown [UPDATED: 2 Days]
        private const val RECOMMENDATION_COOLDOWN_MS = 2 * 24 * 60 * 60 * 1000L  
        
        // Pool Definitions
        private const val RETRO_THRESHOLD_MS = 30 * 24 * 60 * 60 * 1000L // 30 days
        private const val HOT_SONG_MIN_PLAYS = 5 
        private const val NICHE_SONG_MAX_PLAYS = 1
        
        // Distribution
        private const val RATIO_HOT = 0.40
        private const val RATIO_RELATED = 0.30
        private const val RATIO_RETRO = 0.20
        private const val RATIO_NICHE = 0.10
        
        // [NEW] Artist Constraints
        private const val MAX_SONGS_PER_ARTIST = 4
    }

    data class RecommendationResult(
        val songs: List<Song>,
        val reasons: Map<Long, String>
    )

    // Helper for internal scoring
    private data class ScoredSong(
        val song: Song, 
        val score: Float, 
        val debugReason: String
    )

    fun generateDailyRecommendations(
        allSongs: List<Song>,
        statsMap: Map<Long, SongStatistics>,
        currentDevice: String = "UNKNOWN" 
    ): RecommendationResult {
        // [Verified] Min Songs Requirement
        // [Verified] Min Songs Requirement
        if (allSongs.size < 5) {
            return RecommendationResult(emptyList(), emptyMap())
        }

        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        // Context Detection
        val currentTimeKey = when (hour) {
            in 6..10 -> "MORNING"
            in 11..13 -> "NOON"
            in 14..18 -> "AFTERNOON"
            in 19..23 -> "EVENING"
            else -> "LATE_NIGHT"
        }
        
        // 1. Calculate Target Count
        val targetCount = (allSongs.size * RATIO).toInt()
            .coerceIn(MIN_RECOMMENDATION_COUNT, MAX_RECOMMENDATION_COUNT)
            .coerceAtMost(allSongs.size)
            
        // 2. Dynamic Filtering (Strict vs Relaxed)
        // We first try to find enough songs that satisfy the Strict Cooldown.
        // If not enough (< 20), we relax the rules to include ANY available song and disable artist limits.
        
        data class ScoredSong(val song: Song, val score: Float, val debugReason: String)
        
        val strictCandidatesRaw = mutableListOf<Song>()
        val cooldownCandidatesRaw = mutableListOf<Song>()
        
        allSongs.forEach { song ->
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            val timeSinceRecommended = now - stats.lastRecommendedAt
            
            // Check Cooldown
            if (stats.lastRecommendedAt != 0L && timeSinceRecommended < RECOMMENDATION_COOLDOWN_MS) {
                cooldownCandidatesRaw.add(song)
            } else {
                strictCandidatesRaw.add(song)
            }
        }
        
        // Determine Mode
        val useRelaxedRules = strictCandidatesRaw.size < MIN_RECOMMENDATION_COUNT
        
        // Final Candidate Pool
        val finalRawCandidates = if (useRelaxedRules) {
            strictCandidatesRaw + cooldownCandidatesRaw // Use everything if strict is not enough
        } else {
            strictCandidatesRaw
        }
        
        // Score Candidates
        val candidates = finalRawCandidates.mapNotNull { song ->
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            
            // Scoring Logic
            var score = 1.0f
            var reasons = mutableListOf<String>()
            
            // A. Recency / Freshness (Higher for recent, penalized for very old if not retro)
            // (Simulated logic: newer songs get slight boost)
            
            // B. Context Match (Time of Day)
            val timePlayCount = stats.timeOfDayStats?.get(currentTimeKey) ?: 0
            if (timePlayCount > 2) {
                score += 2.0f
                reasons.add("时段(${currentTimeKey})")
            }
            
            // C. Device Match
            if (currentDevice != "UNKNOWN" && stats.lastDevice == currentDevice) {
                score += 1.5f
                reasons.add("设备偏好")
            }
            
            // D. Engagement
            val totalPlays = stats.playCount + stats.skipCount
            if (totalPlays > 5) {
                val completionRate = stats.completionCount.toFloat() / totalPlays
                if (completionRate > 0.8f) {
                    score += 1.0f
                    reasons.add("高完播")
                } else if (completionRate < 0.2f) {
                    score -= 2.0f // Penalize frequent skips
                }
            }
            
            // [NEW] E. Recommendation Feedback (Explicit Feedback)
            val totalRecPlays = stats.recommendationPlayCount + stats.recommendationSkipCount
            if (totalRecPlays > 0) {
                val recCompletionRate = stats.recommendationCompleteCount.toFloat() / totalRecPlays
                if (recCompletionRate > 0.8f) {
                    score += 1.5f // Stronger bonus for verifying recommendation was good
                    reasons.add("推荐反馈好")
                } else if (recCompletionRate < 0.3f) {
                   score -= 3.0f // Strong penalty if user consistently skips this when recommended
                   reasons.add("推荐常跳过")
                }
            }
            
            // F. Play Count (Base Popularity)
            if (stats.playCount > 10) score += 1.0f
            
            // [NEW] G. Strict Priority Boost
            // If we are in "Relaxed Result" mode (mixed pool), we must prioritize STRICT candidates first.
            if (stats.lastRecommendedAt == 0L || (now - stats.lastRecommendedAt) >= RECOMMENDATION_COOLDOWN_MS) {
                score += 50.0f // Huge boost ensures they are picked before any "Relaxed" (Cooldown-Violating) songs
            }
            
            // [FIX] Pre-compute random jitter for stable sorting
            val jitter = Math.random().toFloat() * 1.5f
            val finalScore = score + jitter
            
            ScoredSong(song, finalScore, reasons.joinToString(","))
        }

        if (candidates.isEmpty()) return RecommendationResult(emptyList(), emptyMap())

        // 3. Categorize into Pools (using Scores now!)
        val hotCandidates = mutableListOf<ScoredSong>()
        val retroCandidates = mutableListOf<ScoredSong>()
        val nicheCandidates = mutableListOf<ScoredSong>()
        val remainderCandidates = mutableListOf<ScoredSong>() 
        
        val hotArtists = mutableSetOf<String>()
        
        candidates.forEach { item ->
            val song = item.song
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            
            if (stats.playCount >= HOT_SONG_MIN_PLAYS) {
                // Hot Pool now prioritizes High Scores
                hotCandidates.add(item)
                hotArtists.add(song.artist)
            } else if (stats.playCount > 0 && (now - stats.lastPlayedAt) > RETRO_THRESHOLD_MS) {
                retroCandidates.add(item)
            } else if (stats.playCount <= NICHE_SONG_MAX_PLAYS) {
                nicheCandidates.add(item)
            } else {
                remainderCandidates.add(item)
            }
        }
        
        val relatedCandidates = (retroCandidates + nicheCandidates + remainderCandidates).filter { 
            hotArtists.contains(it.song.artist) 
        }.toMutableList()
        
        // 4. Calculate Quotas (Same as before)
        var quotaHot = (targetCount * RATIO_HOT).toInt()
        var quotaRelated = (targetCount * RATIO_RELATED).toInt()
        var quotaRetro = (targetCount * RATIO_RETRO).toInt()
        var quotaNiche = (targetCount * RATIO_NICHE).toInt()
        
        val totalAllocated = quotaHot + quotaRelated + quotaRetro + quotaNiche
        val diff = targetCount - totalAllocated
        if (diff > 0) quotaHot += diff 
        
        // 5. Fill Pools with WEIGHTED SELECTION
        val finalSelection = mutableListOf<Song>()
        val reasons = HashMap<Long, String>()
        val selectedIds = mutableSetOf<Long>()
        val selectedContentKeys = mutableSetOf<String>() 
        val artistCounts = HashMap<String, Int>() 
        
        fun selectForPool(poolItems: List<ScoredSong>, quota: Int, reasonPrefix: String): Int {
            if (quota <= 0) return 0
            
            // Sort by Score Descending -> Then Shuffle top chunk to keep variety?
            // Or Weighted Random selection? 
            // Let's use Deterministic Sort by Score (Score already includes random jitter now)
            val sortedItems = poolItems.sortedByDescending { it.score } 
            
            var addedCount = 0
            
            for (item in sortedItems) {
                val song = item.song
                if (addedCount >= quota) break
                
                // Diversity Checks
                val currentArtistCount = artistCounts.getOrDefault(song.artist, 0)
                if (!useRelaxedRules && currentArtistCount >= MAX_SONGS_PER_ARTIST) continue // Relax Limit check if Relaxed Mode is Active

                val contentKey = "${song.title.trim()}|${song.artist.trim()}"
                if (selectedContentKeys.contains(contentKey)) continue
                if (selectedIds.contains(song.id)) continue
                
                finalSelection.add(song)
                selectedIds.add(song.id)
                selectedContentKeys.add(contentKey)
                
                // Construct Reason
                val specificReason = if (item.debugReason.isNotEmpty()) " - ${item.debugReason}" else ""
                reasons[song.id] = "$reasonPrefix$specificReason"
                
                artistCounts[song.artist] = currentArtistCount + 1
                addedCount++
            }
            return quota - addedCount 
        }
        
        // -- Filling Phases
        var deficitHot = selectForPool(hotCandidates, quotaHot, "常听热歌")
        
        var deficitRelated = selectForPool(relatedCandidates, quotaRelated + deficitHot, "歌手关联")
        
        var deficitRetro = selectForPool(retroCandidates, quotaRetro + deficitRelated, "许久未听")
        
        var deficitNiche = selectForPool(nicheCandidates, quotaNiche + deficitRetro, "冷门佳作")
        
        // Final Sweep
        if (deficitNiche > 0) {
            val allRemaining = candidates.filter { !selectedIds.contains(it.song.id) }
            selectForPool(allRemaining, deficitNiche, "猜你喜欢")
        }

        // Return Result
        return RecommendationResult(
            songs = finalSelection.shuffled(), // Shuffle final list for display
            reasons = reasons
        )
    }
}
