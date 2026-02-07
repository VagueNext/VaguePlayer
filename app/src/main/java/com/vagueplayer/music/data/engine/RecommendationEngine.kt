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

    fun generateDailyRecommendations(
        allSongs: List<Song>,
        statsMap: Map<Long, SongStatistics>,
        currentDevice: String? = null 
    ): RecommendationResult {
        // [NEW] Min Songs Requirement
        if (allSongs.size < 100) {
            return RecommendationResult(emptyList(), emptyMap())
        }

        if (allSongs.isEmpty()) {
            return RecommendationResult(emptyList(), emptyMap())
        }

        val now = System.currentTimeMillis()
        
        // 1. Calculate Target Count
        val targetCount = (allSongs.size * RATIO).toInt()
            .coerceIn(MIN_RECOMMENDATION_COUNT, MAX_RECOMMENDATION_COUNT)
            .coerceAtMost(allSongs.size)
            
        // 2. Filter Cooldown (2 Days)
        val availableSongs = allSongs.filter { song ->
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            val timeSinceRecommended = now - stats.lastRecommendedAt
            timeSinceRecommended > RECOMMENDATION_COOLDOWN_MS || stats.lastRecommendedAt == 0L
        }

        if (availableSongs.isEmpty()) return RecommendationResult(emptyList(), emptyMap())

        // 3. Categorize into Pools
        val hotCandidates = mutableListOf<Song>()
        val retroCandidates = mutableListOf<Song>()
        val nicheCandidates = mutableListOf<Song>()
        val remainderCandidates = mutableListOf<Song>() 
        
        val hotArtists = mutableSetOf<String>()
        
        availableSongs.forEach { song ->
            val stats = statsMap[song.id] ?: SongStatistics(song.id)
            
            if (stats.playCount >= HOT_SONG_MIN_PLAYS) {
                hotCandidates.add(song)
                hotArtists.add(song.artist)
            } else if (stats.playCount > 0 && (now - stats.lastPlayedAt) > RETRO_THRESHOLD_MS) {
                retroCandidates.add(song)
            } else if (stats.playCount <= NICHE_SONG_MAX_PLAYS) {
                nicheCandidates.add(song)
            } else {
                remainderCandidates.add(song)
            }
        }
        
        val relatedCandidates = (retroCandidates + nicheCandidates + remainderCandidates).filter { 
            hotArtists.contains(it.artist) 
        }.toMutableList()
        
        // 4. Calculate Quotas
        var quotaHot = (targetCount * RATIO_HOT).toInt()
        var quotaRelated = (targetCount * RATIO_RELATED).toInt()
        var quotaRetro = (targetCount * RATIO_RETRO).toInt()
        var quotaNiche = (targetCount * RATIO_NICHE).toInt()
        
        val totalAllocated = quotaHot + quotaRelated + quotaRetro + quotaNiche
        val diff = targetCount - totalAllocated
        if (diff > 0) quotaHot += diff 
        
        // 5. Fill Pools & Handle Overflow
        val finalSelection = mutableListOf<Song>()
        val reasons = HashMap<Long, String>()
        val selectedIds = mutableSetOf<Long>()
        val selectedContentKeys = mutableSetOf<String>() // [NEW] For Title+Artist deduplication
        val artistCounts = HashMap<String, Int>() // [NEW] Track artist frequency
        
        fun selectForPool(candidates: List<Song>, quota: Int, reason: String, poolName: String): Int {
            if (quota <= 0) return 0
            
            val validCandidates = candidates.filter { !selectedIds.contains(it.id) }.shuffled()
            var addedCount = 0
            
            for (song in validCandidates) {
                if (addedCount >= quota) break
                
                // [NEW] Check Artist Diversity
                val currentArtistCount = artistCounts.getOrDefault(song.artist, 0)
                if (currentArtistCount >= MAX_SONGS_PER_ARTIST) continue

                // [NEW] Check Title/Artist Duplicates (Content Deduplication)
                // Use a composite key to ensure we don't add the same song (different file) twice
                val contentKey = "${song.title.trim()}|${song.artist.trim()}"
                if (selectedContentKeys.contains(contentKey)) continue
                
                finalSelection.add(song)
                selectedIds.add(song.id)
                selectedContentKeys.add(contentKey)
                reasons[song.id] = reason
                artistCounts[song.artist] = currentArtistCount + 1
                addedCount++
            }
            return quota - addedCount // Returning deficit
        }
        
        // Logic: Try to fill quota. If not enough, pass deficit to next pool priority.
        // Priority: Hot -> Related -> Niche -> Retro -> Hot (Loop back)
        
        // -- Phase 1: Fill Hot (Priority 1)
        var deficitHot = selectForPool(hotCandidates, quotaHot, "常听热歌", "HOT")
        
        // -- Phase 2: Fill Related (Priority 2) + Deficit from Hot
        var deficitRelated = selectForPool(relatedCandidates, quotaRelated + deficitHot, "歌手关联", "RELATED")
        
        // -- Phase 3: Fill Retro (Priority 3) + Deficit from Related
        // Note: Retro candidates might have been taken by Related if we allowed overlap. 
        // Since we check `selectedIds`, it's safe.
        var deficitRetro = selectForPool(retroCandidates, quotaRetro + deficitRelated, "许久未听", "RETRO")
        
        // -- Phase 4: Fill Niche (Priority 4) + Deficit from Retro
        var deficitNiche = selectForPool(nicheCandidates, quotaNiche + deficitRetro, "冷门佳作", "NICHE")
        
        // -- Phase 5: Final Sweep (If logic was super strict or candidates sparse)
        // If we still have deficit (deficitNiche > 0), try to fill from ANY remaining available song
        if (deficitNiche > 0) {
            val allRemaining = availableSongs.filter { !selectedIds.contains(it.id) }.shuffled()
            selectForPool(allRemaining, deficitNiche, "猜你喜欢", "FILL")
        }

        // 6. Return Result
        return RecommendationResult(
            songs = finalSelection.shuffled(),
            reasons = reasons
        )
    }
}
