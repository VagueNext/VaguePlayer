package com.vagueplayer.music.data.analysis

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.data.model.SongStatistics
import com.vagueplayer.music.data.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class PlaybackAnalyzer(
    private val context: Context,
    private val repository: PlaylistRepository
) {
    companion object {
        // Thresholds
        private const val IGNORE_THRESHOLD_MS = 5000L      // < 5s: Ignore
        private const val SKIP_THRESHOLD_MS = 30000L       // 5s <= x < 30s: Skip
        private const val CUT_RATIO = 0.6f                 // >= 30s && < 60%: Cut
        private const val COMPLETION_RATIO = 0.9f          // >= 90%: Complete
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current Session State
    private var currentSongId: Long? = null
    private var currentSongDuration: Long = 0
    private var startTime: Long = 0
    private var maxPosition: Long = 0
    private var isRecommended: Boolean = false

    fun onSongChanged(newSong: Song?, isRecommendation: Boolean = false) {
        // 1. Analyze previous song session
        if (currentSongId != null) {
            analyzeSession()
        }

        // 2. Start new session
        if (newSong != null) {
            currentSongId = newSong.id
            currentSongDuration = newSong.duration
            startTime = System.currentTimeMillis()
            maxPosition = 0
            isRecommended = isRecommendation
        } else {
            currentSongId = null
            currentSongDuration = 0
        }
    }

    fun onPlaybackProgress(currentPosition: Long) {
        if (currentPosition > maxPosition) {
            maxPosition = currentPosition
        }
    }
    
    // Called when user actively clicks "Next" or selects another song manually
    fun onUserSkip(isManual: Boolean) {
         // This could be used to set a flag "explicit skip" vs "natural end"
    }

    fun onStopped() {
        if (currentSongId != null) {
            analyzeSession()
            currentSongId = null
        }
    }

    private fun analyzeSession() {
        val songId = currentSongId ?: return
        val playedDuration = maxPosition
        val totalDuration = currentSongDuration
        val wasRecommended = isRecommended
        
        // Don't record very short blips
        if (playedDuration < IGNORE_THRESHOLD_MS) return

        scope.launch {
            repository.updateStatistics(songId) { stats ->
                var newStats = stats.copy(
                    playCount = stats.playCount + 1,
                    totalPlayDuration = stats.totalPlayDuration + playedDuration,
                    lastPlayedAt = System.currentTimeMillis()
                )

                // 1. Analyze Completion Type
                if (playedDuration < SKIP_THRESHOLD_MS) {
                    newStats = newStats.copy(skipCount = newStats.skipCount + 1)
                    if (wasRecommended) {
                        newStats = newStats.copy(recommendationSkipCount = newStats.recommendationSkipCount + 1)
                    }
                } else if (totalDuration > 0 && (playedDuration.toFloat() / totalDuration) < CUT_RATIO) {
                    newStats = newStats.copy(cutCount = newStats.cutCount + 1)
                } else if (totalDuration > 0 && (playedDuration.toFloat() / totalDuration) >= COMPLETION_RATIO) {
                    newStats = newStats.copy(completionCount = newStats.completionCount + 1)
                    if (wasRecommended) {
                        newStats = newStats.copy(recommendationCompleteCount = newStats.recommendationCompleteCount + 1)
                    }
                }
                
                if (wasRecommended) {
                    newStats = newStats.copy(recommendationPlayCount = newStats.recommendationPlayCount + 1)
                }

                // 2. Time Context
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val timeKey = when (hour) {
                    in 5..10 -> "MORNING"
                    in 11..13 -> "NOON"
                    in 14..18 -> "AFTERNOON" // Added for granularity
                    in 19..23 -> "NIGHT"
                    else -> "LATE_NIGHT"
                }
                val newTimeStats = (newStats.timeOfDayStats ?: emptyMap()).toMutableMap()
                newTimeStats[timeKey] = (newTimeStats[timeKey] ?: 0) + 1
                
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val dayKey = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) "WEEKEND" else "WEEKDAY"
                val newDayStats = (newStats.dayOfWeekStats ?: emptyMap()).toMutableMap()
                newDayStats[dayKey] = (newDayStats[dayKey] ?: 0) + 1

                newStats = newStats.copy(
                    timeOfDayStats = newTimeStats,
                    dayOfWeekStats = newDayStats
                )
                
                // 3. Last 5 Days History (Sliding Window could be complex, simple list append for now)
                // We just append timestamps of plays/skips. Engine will filter old ones.
                val newPlays = (newStats.lastFiveDaysPlays ?: emptyList()).toMutableList()
                newPlays.add(System.currentTimeMillis())
                // Keep list size reasonable (e.g. last 50 entries) to prevent bloat, Engine filters by time
                if (newPlays.size > 50) newPlays.removeAt(0)
                
                val newSkips = (newStats.lastFiveDaysSkips ?: emptyList()).toMutableList()
                if (playedDuration < SKIP_THRESHOLD_MS) {
                    newSkips.add(System.currentTimeMillis())
                    if (newSkips.size > 50) newSkips.removeAt(0)
                }
                
                // 4. Device Context
                val deviceType = getAudioOutputDevice()
                newStats = newStats.copy(
                    lastDevice = deviceType,
                    lastFiveDaysPlays = newPlays,
                    lastFiveDaysSkips = newSkips
                )

                newStats
            }
        }
    }

    private fun getAudioOutputDevice(): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                return "HEADPHONES"
            }
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                return "BLUETOOTH"
            }
        }
        return "SPEAKER"
    }
}
