package com.vagueplayer.music.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.data.repository.FolderRepository
import com.vagueplayer.music.data.repository.MusicRepository
import com.vagueplayer.music.data.repository.PlaylistRepository
import com.vagueplayer.music.service.PlaybackService
import com.vagueplayer.music.data.engine.RecommendationEngine
import com.vagueplayer.music.data.model.RecommendationState
import com.vagueplayer.music.data.analysis.PlaybackAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive // [NEW] Explicit Import
import kotlinx.coroutines.Job // [NEW] Explicit Import
import java.util.Calendar

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null, // For bilingual lyrics (e.g., Chinese translation)
    val syllables: List<Pair<Long, String>>? = null // For karaoke: start time -> syllable text
)

class AudioViewModel(
    private val context: Context, // Application Context
    private val musicRepository: MusicRepository,
    private val folderRepository: FolderRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager


    // Playback Analytics (NEW)
    private val playbackAnalyzer = PlaybackAnalyzer(context, playlistRepository)
    
    // Playback session tracking
    private var currentSessionStart: Long = 0
    private var currentSessionSongId: Long? = null
    private var currentRecommendationContext: Boolean = false // [NEW] Track if playing from recommendations

    // Sleep Timer Support
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val _sleepTimerRemaining = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: kotlinx.coroutines.flow.StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return

        sleepTimerJob = viewModelScope.launch {
            val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _sleepTimerRemaining.value = null
                    mediaController?.pause() // Stop playback
                    break
                }
                _sleepTimerRemaining.value = remaining
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
    }

    fun toggleSortDialog() {
        _showSortDialog.value = !_showSortDialog.value
    }
    
    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog = _showSortDialog.asStateFlow()

    // [NEW] Daily Recommendation Cover Rotation
    private val _dailyCoverIndex = MutableStateFlow(0)

    init {
        // Start Rotation Timer
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(15 * 60 * 1000L) // 15 Minutes
                _dailyCoverIndex.value++
            }
        }
    }
    
    // Sorting
    enum class SortOption {
        TITLE, ARTIST, SIZE, PLAY_COUNT, DURATION_ASC, DURATION_DESC, DATE_ADDED, CUSTOM
    }
    
    private val _sortOption = MutableStateFlow(SortOption.TITLE) // Main Library Sort
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    
    // Per-Playlist Sort Options (Context Independent)
    private val _playlistSortOptions = MutableStateFlow<Map<String, SortOption>>(emptyMap()) // Key: Context/ID
    val playlistSortOptions: StateFlow<Map<String, SortOption>> = _playlistSortOptions.asStateFlow() // [NEW] Expose for observation
    
    fun getSortOption(contextId: String): SortOption {
        return _playlistSortOptions.value[contextId] ?: SortOption.TITLE // Default to Title
    }
    
    // [NEW] Global Dialog State for Playlist Deletion (Hoisted for correct Z-Order)
    private val _playlistToDelete = MutableStateFlow<Playlist?>(null)
    val playlistToDelete: StateFlow<Playlist?> = _playlistToDelete.asStateFlow()

    fun requestDeletePlaylist(playlist: Playlist) {
        Log.d("AudioViewModel", "Requesting delete for playlist: ${playlist.name}")
        _playlistToDelete.value = playlist
    }

    fun cancelDeletePlaylist() {
        _playlistToDelete.value = null
    }

    fun confirmDeletePlaylist() {
        _playlistToDelete.value?.let { playlist ->
            viewModelScope.launch {
                deletePlaylist(playlist.id) // [FIX] Call local method, not repo
                // Refresh if needed, or Flow will handle it
            }
        }
        _playlistToDelete.value = null
    }

    fun setSortOption(option: SortOption, contextId: String? = null) {
        if (contextId == null || contextId == "LIBRARY") {
            _sortOption.value = option
            resortSongs() // Sorts the main library list
        } else {
            // Update map for specific context
            val current = _playlistSortOptions.value.toMutableMap()
            current[contextId] = option
            _playlistSortOptions.value = current
        }
    }
    
    // Helper to sort a specific list based on an option
    fun sortSongs(songs: List<Song>, option: SortOption): List<Song> {
        return when (option) {
            SortOption.CUSTOM -> songs // Playlists might have custom order, but if 'Custom' is selected as a sort, usually means 'Default/Manual'
            SortOption.TITLE -> songs.sortedBy { com.vagueplayer.music.utils.PinyinUtils.toPinyin(it.title) }
            SortOption.ARTIST -> songs.sortedBy { com.vagueplayer.music.utils.PinyinUtils.toPinyin(it.artist) }
            SortOption.SIZE -> songs.sortedByDescending { it.size }
            SortOption.PLAY_COUNT -> songs.sortedByDescending { _playCounts.value[it.id] ?: 0 }
            SortOption.DURATION_ASC -> songs.sortedBy { it.duration }
            SortOption.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SortOption.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        }
    }


    // Loop Count Logic
    fun setLoopCount(count: Int) {
        if (count > 0) {
           _targetLoopCount.value = count
           _remainingLoopCount.value = count
           // Force Repeat One mode if setting a specific count
           mediaController?.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            // Reset
            _targetLoopCount.value = 0
            _remainingLoopCount.value = 0
            mediaController?.repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Lyrics Search
    // --- SEARCH LOGIC (Async Two-Tier) ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Combined Result State
    data class SearchUiState(
        val meta: List<Song> = emptyList(),
        val lyrics: List<Song> = emptyList(),
        val isSearchingLyrics: Boolean = false
    )
    
    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()
    private var currentSearchToken: String = ""

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performDualTrackSearch(query)
    }

    private fun performDualTrackSearch(query: String) {
        val token = java.util.UUID.randomUUID().toString()
        currentSearchToken = token
        
        if (query.isBlank()) {
            _searchUiState.value = SearchUiState()
            return
        }

        // TRACK 1: Metadata (Instant)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val metaResults = musicRepository.searchMeta(query)
            
            // Check Token (Audit Fix)
            if (currentSearchToken == token) {
                // Update UI immediately with Track 1
                _searchUiState.value = _searchUiState.value.copy(
                    meta = metaResults,
                    lyrics = emptyList(), // Clear old lyrics on new search
                    isSearchingLyrics = true
                )
            }
        }

        // TRACK 2: Lyrics (Delayed)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Debounce
            kotlinx.coroutines.delay(300) 
            
            // Re-check Token
            if (currentSearchToken != token) return@launch
            
            val lyricsResults = musicRepository.searchLyrics(query)
            
            // Final Token Check
            if (currentSearchToken == token) {
                 _searchUiState.value = _searchUiState.value.copy(
                    lyrics = lyricsResults,
                    isSearchingLyrics = false
                )
            }
        }
    }
    
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentPlaylists = _userPlaylists.value.toMutableList()
            currentPlaylists.removeAll { it.id == playlistId }
            playlistRepository.savePlaylists(currentPlaylists)
            _userPlaylists.value = playlistRepository.loadPlaylists()
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentPlaylists = _userPlaylists.value.toMutableList()
            val index = currentPlaylists.indexOfFirst { it.id == playlistId }
            if (index != -1) {
                val old = currentPlaylists[index]
                currentPlaylists[index] = old.copy(name = newName)
                playlistRepository.savePlaylists(currentPlaylists)
                _userPlaylists.value = playlistRepository.loadPlaylists()
            }
        }
    }

    // Playlist Import/Export
    fun importPlaylistFromTxt(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val lines = inputStream?.bufferedReader()?.readLines() ?: return@launch
                inputStream.close()
                
                if (lines.isEmpty()) return@launch

                val playlistName = "Imported ${System.currentTimeMillis() / 1000}"
                val matchedSongIds = mutableListOf<com.vagueplayer.music.data.model.Song>() // Store full Song objects
                val allSongs = _songs.value
                
                // Match logic: Try to find song by Path (Exact match preferred)
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    
                    // Match by File Path (Robust)
                    var song = allSongs.find { it.path.equals(trimmed, ignoreCase = true) }
                    
                    // Fallback to previous fuzzy match if path fails (for backward compatibility if needed)
                    // But user requested specific format, so prioritized path.
                    if (song == null) {
                         song = allSongs.find { 
                             val key = "${it.title} - ${it.artist}"
                             key.equals(trimmed, ignoreCase = true) 
                         }
                    }
                    
                    if (song != null) {
                        matchedSongIds.add(song)
                    }
                }
                
                if (matchedSongIds.isNotEmpty()) {
                    // Create Playlist Manually
                    val newPlaylist = com.vagueplayer.music.data.model.Playlist(
                        name = playlistName,
                        songs = matchedSongIds.toMutableList()
                    )
                    
                    val currentPlaylists = _userPlaylists.value.toMutableList()
                    currentPlaylists.add(newPlaylist)
                    
                    playlistRepository.savePlaylists(currentPlaylists)
                    // Refresh
                    _userPlaylists.value = playlistRepository.loadPlaylists()
                    Log.d("AudioViewModel", "Imported playlist '$playlistName' with ${matchedSongIds.size} songs.")
                }
            } catch (e: Exception) {
                Log.e("AudioViewModel", "Failed to import playlist", e)
            }
        }
    }

    // Placeholder for UI calls
    fun exportPlaylist(playlistId: String, name: String) {
        Log.d("AudioViewModel", "Export requested for $name ($playlistId). Needs File Picker implementation.")
    }
    
    fun exportPlaylistToTxt(playlistId: String, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val playlist = _userPlaylists.value.find { it.id == playlistId } ?: return@launch
                val sb = StringBuilder()
                
                for (song in playlist.songs) {
                    // Export Format: Absolute Path
                    // Fallback to "Title - Artist" if path is missing (shouldn't happen with new logic)
                    if (song.path.isNotBlank()) {
                        sb.append("${song.path}\n")
                    } else {
                         sb.append("${song.title} - ${song.artist}\n")
                    }
                }
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(sb.toString().toByteArray())
                }
                 Log.d("AudioViewModel", "Exported playlist '${playlist.name}' to ${uri.path}")
            } catch (e: Exception) {
                Log.e("AudioViewModel", "Failed to export playlist", e)
            }
        }
    }


    // Cache for searched lyrics to avoid re-opening files
    private val lyricsCache = mutableMapOf<Long, String>()

    // Existing methods match...

    fun searchSongsWithLyrics(query: String) {
        if (query.isBlank()) {
            _searchUiState.value = SearchUiState()
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allSongs = _songs.value
            val filtered = allSongs.filter { song ->
                // 1. Basic Metadata Match
                if (song.title.contains(query, ignoreCase = true) || song.artist.contains(query, ignoreCase = true)) {
                    return@filter true
                }
                
                // 2. Embedded/External Lyrics Match
                // Check cache first
                val cached = lyricsCache[song.id]
                if (cached != null) {
                    return@filter cached.contains(query, ignoreCase = true)
                }
                
                // Extract from File (Embedded or LRC) via LrcParser
                try {
                    val lyricsList = com.vagueplayer.music.data.lyrics.LrcParser.loadLyricsForSong(context, song.contentUri)
                    if (lyricsList.isNotEmpty()) {
                        // Concatenate all text for searching
                        val fullLyrics = lyricsList.joinToString(" ") { it.text + " " + (it.translation ?: "") }
                        
                        // Cache it (limit cache size in real app, but ok for now)
                        lyricsCache[song.id] = fullLyrics
                        
                        return@filter fullLyrics.contains(query, ignoreCase = true)
                    }
                } catch (e: Exception) {
                    // Ignore errors, skip song
                }
                
                false 
            }
            // Verify token is still valid
            if (currentSearchToken == currentSearchToken) { // simplified check, logic handled in caller mainly
                 _searchUiState.value = _searchUiState.value.copy(
                    meta = filtered,
                    // Lyrics search is separate
                )
            }
        }
    }
    
    // showSortDialog is already here
    
    fun resortSongs() {
        val currentSongs = _songs.value
        if (currentSongs.isEmpty()) return
        
        // DEBUG: Log Sort Action
        Log.d("AudioViewModel", "Resorting songs with option: ${_sortOption.value}")
        
        val sorted = when (_sortOption.value) {
            SortOption.CUSTOM -> {
                // Fallback to Pinyin Title Sort to prevent unsorted list
                currentSongs.sortedBy { 
                    com.vagueplayer.music.utils.PinyinUtils.toPinyin(it.title)
                }
            }
            SortOption.TITLE -> {
                // Strict Pinyin Sort
                val s = currentSongs.sortedBy { 
                    val key = com.vagueplayer.music.utils.PinyinUtils.toPinyin(it.title)
                    key
                }
                if (s.isNotEmpty()) {
                     Log.d("AudioViewModel", "First 5 keys: ${s.take(5).map { com.vagueplayer.music.utils.PinyinUtils.toPinyin(it.title) }}")
                }
                s
            }
            SortOption.ARTIST -> {
                 currentSongs.sortedBy { com.vagueplayer.music.utils.PinyinUtils.toPinyin(it.artist) }
            }
            // ... other options
            SortOption.SIZE -> currentSongs.sortedByDescending { it.size }
            SortOption.PLAY_COUNT -> currentSongs.sortedByDescending { _playCounts.value[it.id] ?: 0 }
            SortOption.DURATION_ASC -> currentSongs.sortedBy { it.duration }
            SortOption.DURATION_DESC -> currentSongs.sortedByDescending { it.duration }
            SortOption.DATE_ADDED -> currentSongs.sortedByDescending { it.dateAdded }
        }
        _songs.value = sorted
        Log.d("AudioViewModel", "Sorted ${sorted.size} songs.")
    }

    // Default to TITLE to ensure Pinyin Sort is active by default
    // private val _sortOption = MutableStateFlow(SortOption.TITLE) // MOVED TO DECLARATION SITE

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _dailyMix = MutableStateFlow<List<Song>>(emptyList())
    val dailyMix: StateFlow<List<Song>> = _dailyMix.asStateFlow()

    private val _guessYouLike = MutableStateFlow<List<Song>>(emptyList())
    val guessYouLike: StateFlow<List<Song>> = _guessYouLike.asStateFlow()

    // Search Logic
    // private val _searchQuery = MutableStateFlow("") // Duplicate definition exists above at line 93, so DO NOT restore this one.

    // CONSOLIDATED SEARCH FUNCTION ALREADY EXISTS

    // Playback State
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _progress = MutableStateFlow(0f) 
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Loop Count Logic
    private val _targetLoopCount = MutableStateFlow(0) // 0 = infinite in repeat one
    val targetLoopCount: StateFlow<Int> = _targetLoopCount.asStateFlow()
    
    private val _remainingLoopCount = MutableStateFlow(0)
    val remainingLoopCount: StateFlow<Int> = _remainingLoopCount.asStateFlow()

    // Loop Tracking State
    private var lastMediaId: String? = null

    // Lyrics State
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()
    
    private val _currentLyricIndex = MutableStateFlow(0)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()



    // MediaController
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Consolidated init block is below

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                // Sync initial state
                mediaController?.let { controller ->
                    _isPlaying.value = controller.isPlaying
                    // FIXED: Don't read from controller, use saved value instead
                    // Apply saved repeatMode and shuffleMode to Service
                    controller.repeatMode = _repeatMode.value
                    controller.shuffleModeEnabled = _isShuffleEnabled.value
                    
                    // Sync Settings to Service
                    setMixAudioEnabled(_isMixAudioEnabled.value)
                    // REMOVED: Gapless sync (ExoPlayer handles gapless by default)
                    
                    // Restore current song if possible, or just wait for listener
                    tryRestoreState() // Try restoring after controller connects
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private fun setupPlayerListener() {
        var lastKnownIndex = -1 // Track previous index
        var wasOnLastSong = false // Track if previous song was the last in shuffle order

        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (!isPlaying) {
                    // Analyze session when playback stops/pauses
                    playbackAnalyzer.onStopped()
                    savePlaybackState() // Save on pause
                }
            }
            
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                 updateCurrentQueueFromController()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                 val newMediaId = mediaItem?.mediaId
                 // Reset counters if we switched to a DIFFERENT song
                 if (newMediaId != lastMediaId) {
                     _targetLoopCount.value = 0
                     _remainingLoopCount.value = 0
                 }
                 lastMediaId = newMediaId
                 
                 // Track song change in analytics
                 val newSong = mediaItem?.let { item ->
                     _songs.value.find { it.id.toString() == item.mediaId }
                 }
                 // Check if this song was from recommendation (would need to track this separately)
                 playbackAnalyzer.onSongChanged(newSong, isRecommendation = false)
                 
                 updateCurrentSongFromController()
                 
                 // [PREEMPTIVE SHUFFLE] When landing on the last song, reshuffle
                 val controller = mediaController ?: return
                 val currentIndex = controller.currentMediaItemIndex
                 val timeline = controller.currentTimeline
                 
                 if (_isShuffleEnabled.value && !timeline.isEmpty && timeline.windowCount > 1) {
                     // Check if CURRENT song is the last in shuffle order
                     val nextIndex = timeline.getNextWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, true)
                     val isCurrentLastSong = (nextIndex == androidx.media3.common.C.INDEX_UNSET)
                     
                     // CRITICAL: Only reshuffle when AUTO-PLAYING to the last song (连播到最后一首)
                     // NOT when user manually selects the last song (用户手动选择最后一首)
                     val isAutoPlayback = (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                     
                     // When we AUTO-PLAY into the last song (and weren't there before), force reshuffle
                     if (isCurrentLastSong && !wasOnLastSong && isAutoPlayback) {
                          android.util.Log.d("SmartShuffle", "Auto-played to last song (pos $currentIndex). Force reshuffle...")
                          viewModelScope.launch {
                              // Save current playlist
                              val items = mutableListOf<MediaItem>()
                              for (i in 0 until timeline.windowCount) {
                                  val window = androidx.media3.common.Timeline.Window()
                                  timeline.getWindow(i, window)
                                  items.add(window.mediaItem)
                              }
                              
                              // Clear and re-add to force new shuffle seed
                              controller.clearMediaItems()
                              kotlinx.coroutines.delay(50)
                              controller.setMediaItems(items)
                              controller.prepare()
                              controller.play()
                              
                              updateCurrentQueueFromController()
                              android.util.Log.d("SmartShuffle", "Forced reshuffle complete. New order generated.")
                          }
                     }
                     
                     // Update flag for next transition
                     wasOnLastSong = isCurrentLastSong
                 }
                 
                 lastKnownIndex = currentIndex
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                 // No-op
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration ?: 0L
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleModeEnabled
                android.util.Log.d("SmartShuffle", "onShuffleModeEnabledChanged: $shuffleModeEnabled")
            }
        })
    }
    
    private fun updateCurrentQueueFromController() {
        val controller = mediaController ?: return
        val timeline = controller.currentTimeline
        if (timeline.isEmpty) {
             _currentQueue.value = emptyList()
             return
        }

        val queue = mutableListOf<MediaItem>()
        // Always show queue in original (linear) order, even in shuffle mode
        for (i in 0 until timeline.windowCount) {
            val mediaItem = timeline.getWindow(i, androidx.media3.common.Timeline.Window()).mediaItem
            queue.add(mediaItem)
        }
        
        // Log first 3 songs to verify order
        if (queue.isNotEmpty()) {
             val preview = queue.take(3).joinToString { it.mediaMetadata.title.toString() }
             android.util.Log.d("SmartShuffle", "Queue Updated (Linear Order). Size: ${queue.size}. Top 3: $preview")
        }
        
        _currentQueue.value = queue.map { item ->
            Song(
                id = item.mediaId.toLongOrNull() ?: 0L,
                title = item.mediaMetadata.title.toString(),
                artist = item.mediaMetadata.artist.toString(),
                albumArtUri = item.mediaMetadata.artworkUri ?: Uri.EMPTY,
                contentUri = item.requestMetadata.mediaUri ?: Uri.EMPTY,
                duration = 0L, // This will be updated when the song plays
                album = "Unknown Album", // Default
                dateAdded = 0L, // Default
                size = 0L,
                path = "" // Default
            )
        }
    }
    
    fun playFromQueue(index: Int) {
        mediaController?.seekToDefaultPosition(index)
        mediaController?.play()
    }
    
    private fun updateCurrentSongFromController() {
        val currentMediaItem = mediaController?.currentMediaItem
        val mediaId = currentMediaItem?.mediaId
        if (mediaId != null) {
            // End previous song session
            onSongEnded()
            
            // Find song in our list
            val song = _songs.value.find { it.id.toString() == mediaId }
            _currentSong.value = song
            
            // Start new song session
            song?.let { onSongStarted(it.id) }
            
            loadLyrics(song) // Load Lyrics
            savePlaybackState() // Save on song change
        }
    }
    
    private fun loadLyrics(song: Song?) {
        // **CRITICAL**: Immediately clear lyrics when switching songs
        _lyrics.value = emptyList()
        _currentLyricIndex.value = 0
        
        if (song == null) {
            return
        }
        
        // Store current song ID to prevent race conditions
        val targetSongId = song.id
        
        // Try to load real LRC file
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Double-check we're still on the same song
            if (_currentSong.value?.id != targetSongId) {
                Log.d("AudioViewModel", "Song changed during lyrics load, aborting")
                return@launch
            }
            
            val parsedLyrics = com.vagueplayer.music.data.lyrics.LrcParser.loadLyricsForSong(context, song.contentUri)
            
            // Final check before updating
            if (_currentSong.value?.id != targetSongId) {
                Log.d("AudioViewModel", "Song changed after lyrics parsed, aborting")
                return@launch
            }
            
            if (parsedLyrics.isNotEmpty()) {
                _lyrics.value = parsedLyrics
                Log.d("AudioViewModel", "Loaded ${parsedLyrics.size} lyric lines for: ${song.title}")
            } else {
                // Fallback: Show "No lyrics" message
                _lyrics.value = listOf(
                    LyricLine(0, "暂无歌词"),
                    LyricLine(1000, "请将 .lrc 文件放置在歌曲同目录下")
                )
                Log.d("AudioViewModel", "No LRC file found for: ${song.title}")
            }
        }
    }
    
    private fun updateLyricIndex(position: Long) {
        val list = _lyrics.value
        if (list.isEmpty()) {
            _currentLyricIndex.value = 0
            return
        }
        // Find last line that started before current position
        val index = list.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
        if (_currentLyricIndex.value != index) {
            _currentLyricIndex.value = index
        }
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Settings State
        
    val customFolders: StateFlow<List<com.vagueplayer.music.data.model.MusicFolder>> = folderRepository.customFolders
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    // Appearance Settings REMOVED (Enforced Liquid Glass)

    // Audio Focus (Concurrent Playback)
    private val _isMixAudioEnabled = MutableStateFlow(true) // Default ON as requested
    val isMixAudioEnabled: StateFlow<Boolean> = _isMixAudioEnabled.asStateFlow()

    // Sidebar Preference
    private val _isSidebarOnLeft = MutableStateFlow(false) 
    val isSidebarOnLeft: StateFlow<Boolean> = _isSidebarOnLeft.asStateFlow()


    fun setSidebarOnLeft(isLeft: Boolean) {
        _isSidebarOnLeft.value = isLeft
        prefs.edit().putBoolean("sidebar_on_left", isLeft).apply()
    }

    fun setMixAudioEnabled(enabled: Boolean) {
        _isMixAudioEnabled.value = enabled
        
        // Send command to Service
        val command = androidx.media3.session.SessionCommand("SET_MIX_AUDIO", android.os.Bundle.EMPTY)
        val args = android.os.Bundle().apply { putBoolean("enabled", enabled) }
        
        mediaController?.sendCustomCommand(command, args)
    }

    // REMOVED: Gapless Playback control (ExoPlayer provides gapless playback by default)

    // Favorites State
    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val prefs by lazy { 
        context.getSharedPreferences("vague_player_prefs", Context.MODE_PRIVATE) 
    }





    // Hidden Songs (Soft Delete)
    // Hidden Songs (Soft Delete)
    private val _hiddenPaths = MutableStateFlow<Set<String>>(emptySet())
    
    // Store ALL songs (Raw Scan Result)
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    
    // Expose Hidden Songs for "Removed" list

    val hiddenSongs: StateFlow<List<Song>> = kotlinx.coroutines.flow.combine(_allSongs, _hiddenPaths) { all, hidden ->
        all.filter { it.path in hidden }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private fun loadHiddenPaths() {
        val saved = prefs.getStringSet("hidden_paths", emptySet()) ?: emptySet()
        _hiddenPaths.value = saved
    }

    private fun loadFavorites() {
        loadHiddenPaths() // Load hidden paths too
        // REMOVED: loadGaplessSetting()
        val saved = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        _hiddenPaths.value = prefs.getStringSet("hidden_paths", emptySet()) ?: emptySet() // Re-load to be sure
        _favoriteIds.value = saved.mapNotNull { it.toLongOrNull() }.toSet()
    }
    
    // === Song Statistics Management ===
    
    private fun loadSongStatistics() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val stats = playlistRepository.loadSongStatistics()
            _songStatistics.value = stats
        }
    }
    
    // Helper to update statistics efficiently
    private fun updateSongStat(songId: Long, update: (com.vagueplayer.music.data.model.SongStatistics) -> com.vagueplayer.music.data.model.SongStatistics) {
        val currentMap = _songStatistics.value.toMutableMap()
        val currentStats = currentMap[songId] ?: com.vagueplayer.music.data.model.SongStatistics(songId)
        
        var newStats = update(currentStats)
        
        // [NEW] Recommendation Specific Feedback
        if (currentRecommendationContext) {
            // Check what changed to apply same change to recommendation counters
            if (newStats.playCount > currentStats.playCount) {
                 newStats = newStats.copy(recommendationPlayCount = newStats.recommendationPlayCount + 1)
            }
            if (newStats.skipCount > currentStats.skipCount) {
                 newStats = newStats.copy(recommendationSkipCount = newStats.recommendationSkipCount + 1)
            }
            if (newStats.completionCount > currentStats.completionCount) {
                 newStats = newStats.copy(recommendationCompleteCount = newStats.recommendationCompleteCount + 1)
            }
        }
        
        currentMap[songId] = newStats
        _songStatistics.value = currentMap
        
        // Persist (Async)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            playlistRepository.saveSongStatistics(currentMap)
        }
    }

    // === Song Statistics for Recommendation Engine ===
    private val _songStatistics = MutableStateFlow<Map<Long, com.vagueplayer.music.data.model.SongStatistics>>(emptyMap())
    val songStatistics: StateFlow<Map<Long, com.vagueplayer.music.data.model.SongStatistics>> = _songStatistics.asStateFlow()

    // === Daily Recommendation Logic ===
    private val recommendationEngine = RecommendationEngine()
    
    private val _recommendationState = MutableStateFlow(RecommendationState())
    val recommendationState: StateFlow<RecommendationState> = _recommendationState.asStateFlow()

    // Derived flow: Map IDs to actual Song objects
    // Using simple find for now, optimized map lookup could be better for large libraries
    val dailyRecommendations: StateFlow<List<Song>> = kotlinx.coroutines.flow.combine(_recommendationState, _allSongs) { state, allSongs ->
        state.recommendedSongIds.mapNotNull { id -> allSongs.find { it.id == id } }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyRecommendationCover: StateFlow<Song?> = kotlinx.coroutines.flow.combine(
        _dailyCoverIndex, dailyRecommendations
    ) { index: Int, songs: List<Song> ->
        if (songs.isEmpty()) null else songs[index % songs.size]
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)


    private fun loadRecommendationState() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val state = playlistRepository.loadRecommendationState()
            _recommendationState.value = state
        }
    }

    private var refreshJob: Job? = null

    fun refreshDailyRecommendations(force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
             val currentState = _recommendationState.value
             val now = System.currentTimeMillis()
             
             // Logic: Refresh Interval is 8 Hours
             val interval = 8L * 60 * 60 * 1000 
             
             // If forcing, ignore time check. Otherwise, respect nextRefreshTime
             // Also prevent refresh if we just refreshed recently (sanity check)
             if (!force && now < currentState.nextRefreshTime) {
                 Log.d("AudioViewModel", "Skipping Daily Recommendations refresh: Not yet time.")
                 return@launch
             }
             
             Log.d("AudioViewModel", "Refreshing Daily Recommendations (Force=$force)...")
 
             val currentSongs = _allSongs.value
             val stats = _songStatistics.value
             
             if (currentSongs.isEmpty()) {
                 Log.d("AudioViewModel", "Cannot refresh recommendations: No songs loaded.")
                 return@launch
             }
 
             // [NEW] Get Current Device for Context
            val deviceKey = getCurrentAudioDeviceType()

            val result = recommendationEngine.generateDailyRecommendations(currentSongs, stats, currentDevice = deviceKey)
             
             if (result.songs.isNotEmpty()) {
                 val nextTime = now + interval
                 
                 // Update State
                 val newState = currentState.copy(
                     lastRefreshTime = now,
                     nextRefreshTime = nextTime,
                     recommendedSongIds = result.songs.map { it.id },
                     recommendationReasons = result.reasons
                 )
                 
                 _recommendationState.value = newState
                 // Mark these songs as recommended in stats (for cooldown)
                 updateRecommendationStats(result.songs, now)
                 playlistRepository.saveRecommendationState(newState)
                 
                 Log.d("AudioViewModel", "Daily Recommendations Refreshed: ${result.songs.size} songs. Next auto-refresh: ${java.util.Date(nextTime)}")
                 
                 // Schedule next
                 scheduleNextAutoRefresh(nextTime)
             } else {
                 Log.w("AudioViewModel", "Daily Recommendations Generation Failed (Empty Result).")
                 // Retry logic or just keep old schedule?
                 // If we failed, maybe we should try again later or keep the old "nextRefreshTime"
                 // For now, let's just ensure the scheduler is running for the existing next time
                 if (currentState.nextRefreshTime > now) {
                     scheduleNextAutoRefresh(currentState.nextRefreshTime)
                 }
             }
         }
    }
    
    // [NEW] Foreground Scheduler
    // Waits until target time then forces a refresh if app is still alive
    private fun scheduleNextAutoRefresh(targetTime: Long) {
        refreshJob?.cancel() // Cancel previous waiter
        refreshJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val delayMs = targetTime - now
            if (delayMs > 0) {
                Log.d("AudioViewModel", "Scheduling Next Refresh in ${delayMs / 1000 / 60} minutes")
                kotlinx.coroutines.delay(delayMs)
                // Time up! Force check/refresh
                if (isActive) {
                    Log.d("AudioViewModel", "Auto-Refresh Timer Triggered")
                    checkDailyRefresh()
                }
            }
        }
    }
    
    // Track playback session
    private fun onSongStarted(songId: Long) {
        currentSessionStart = System.currentTimeMillis()
        currentSessionSongId = songId
        
        // Update statistics: increment play count and record timestamp
        updateSongStat(songId) { stat ->
            stat.copy(
                playCount = stat.playCount + 1,
                lastPlayedAt = currentSessionStart,
                playbackHistory = ((stat.playbackHistory ?: emptyList()) + currentSessionStart).takeLast(20) // Keep last 20 plays
            )
        }
    }
    
    private fun onSongEnded() {
        val songId = currentSessionSongId ?: return
        val duration = System.currentTimeMillis() - currentSessionStart
        val songDuration = _currentSong.value?.duration ?: 0
        
        if (songDuration > 0) {
            when {
                duration < 30_000 -> {
                    // Skip: Less than 30s
                    // We incremented playCount at start (playSong), so decrement it if skipped quickly? 
                    // Wait, current logic: playSong increments.
                    // If we want "Valid Play", we should NOT increment at start, OR decrement here.
                    // Previous logic (lines 1316) increments at start. 
                    // So here we decrement if it was a skip.
                    updateSongStat(songId) { it.copy(skipCount = it.skipCount + 1, playCount = (it.playCount - 1).coerceAtLeast(0)) }
                    
                    // Update recent skips
                    updateSongStat(songId) { current ->
                        val now = System.currentTimeMillis()
                        val recent = (current.lastFiveDaysSkips ?: emptyList()).filter { t -> (now - t) < 5 * 24 * 3600 * 1000L } + now
                        current.copy(lastFiveDaysSkips = recent)
                    }
                }
                duration >= (songDuration * 0.9) -> {
                    // Completion: Played more than 90%
                    updateSongStat(songId) { it.copy(completionCount = it.completionCount + 1) }
                    // Update Advanced Context Stats (Only for valid/engaged plays)
                    updateAdvancedContextStats(songId)
                }
                else -> {
                     // Cut (30s - 90%)
                     updateSongStat(songId) { it.copy(cutCount = it.cutCount + 1) }
                     // It's still a valid play (listened for >30s), so we keep playCount and update context
                     updateAdvancedContextStats(songId)
                }
            }
        }
        
        currentSessionSongId = null
    }

    // [NEW] Advanced Stats Collection Helper
    private fun updateAdvancedContextStats(songId: Long) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2...
        
        // 1. Time of Day
        val timeKey = when (hour) {
            in 6..10 -> "MORNING"
            in 11..13 -> "NOON"
            in 14..18 -> "AFTERNOON"
            in 19..23 -> "EVENING"
            else -> "LATE_NIGHT"
        }
        
        // 2. Day of Week
        val dayKey = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) "WEEKEND" else "WEEKDAY"
        
        // 3. Device Type
        val deviceKey = getCurrentAudioDeviceType()
        
        updateSongStat(songId) { current ->
            // Update Time Map
            val timeStats = (current.timeOfDayStats ?: emptyMap()).toMutableMap()
            timeStats[timeKey] = (timeStats[timeKey] ?: 0) + 1
            
            // Update Day Map
            val dayStats = (current.dayOfWeekStats ?: emptyMap()).toMutableMap()
            dayStats[dayKey] = (dayStats[dayKey] ?: 0) + 1
            
            // Update Recent Plays
            val recentPlays = (current.lastFiveDaysPlays ?: emptyList()).filter { t -> (now - t) < 5 * 24 * 3600 * 1000L } + now
            
            // Update History (Keep last 20)
            val history = (current.playbackHistory ?: emptyList()) + now
            val trimmedHistory = if (history.size > 20) history.takeLast(20) else history

            current.copy(
                timeOfDayStats = timeStats,
                dayOfWeekStats = dayStats,
                lastDevice = deviceKey,
                lastFiveDaysPlays = recentPlays,
                playbackHistory = trimmedHistory,
                lastPlayedAt = now
            )
        }
    }

    private fun getCurrentAudioDeviceType(): String {
        val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            when (device.type) {
                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                android.media.AudioDeviceInfo.TYPE_BLE_HEADSET,
                android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER,
                android.media.AudioDeviceInfo.TYPE_BLE_BROADCAST -> return "BLUETOOTH"
                
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> return "HEADSET"
            }
        }
        return "SPEAKER"
    }
    
    // [NEW] Update stats when songs are recommended (for Cooldown)
    private fun updateRecommendationStats(songs: List<Song>, time: Long) {
        viewModelScope.launch {
            val currentMap = _songStatistics.value.toMutableMap()
            var changed = false
            
            songs.forEach { song ->
                val stats = currentMap[song.id] ?: com.vagueplayer.music.data.model.SongStatistics(song.id)
                currentMap[song.id] = stats.copy(lastRecommendedAt = time)
                changed = true
            }
            
            if (changed) {
                _songStatistics.value = currentMap
                playlistRepository.saveSongStatistics(currentMap) 
                Log.d("AudioViewModel", "Updated recommendation stats for ${songs.size} songs.")
            }
        }
    }

    fun deleteSongs(songs: List<Song>) {
        viewModelScope.launch {
            // Soft Delete: Add to hidden paths
            val currentHidden = _hiddenPaths.value.toMutableSet()
            songs.forEach { currentHidden.add(it.path) }
            _hiddenPaths.value = currentHidden
            
            // Persist
            prefs.edit().putStringSet("hidden_paths", currentHidden).apply()
            
            // Re-filter explicitly
            updateVisibleSongs()
             
             // No longer calling musicRepository.deleteSongs(songs)
            
            // Refresh logic handled by updateVisibleSongs
            clearSelection()
        }
    }
    
    fun restoreSongs(songs: List<Song>) {
        viewModelScope.launch {
            val currentHidden = _hiddenPaths.value.toMutableSet()
            songs.forEach { currentHidden.remove(it.path) }
            _hiddenPaths.value = currentHidden
            
            // Persist
            prefs.edit().putStringSet("hidden_paths", currentHidden).apply()
            
            // Re-filter
            updateVisibleSongs()
        }
    }
    
    private fun updateVisibleSongs() {
        val all = _allSongs.value
        val hidden = _hiddenPaths.value
        val visible = all.filter { it.path !in hidden }
        _songs.value = visible
        resortSongs() // Re-sort
    }

    fun addCustomFolder(uri: Uri) {
        // Resolve Real Path from SAF URI
        var path = uri.path ?: ""
        if (path.contains("primary:")) {
            path = "/storage/emulated/0/" + path.substringAfter("primary:")
        } else if (path.contains("raw:")) {
            path = path.substringAfter("raw:")
        }
        val folder = com.vagueplayer.music.data.model.MusicFolder(uri, uri.lastPathSegment ?: "Unknown", path)
        viewModelScope.launch {
            folderRepository.addFolder(folder)
            scanMedia()
        }
    }
    
    fun removeCustomFolder(uri: Uri) {
        viewModelScope.launch {
            folderRepository.removeFolder(uri)
            scanMedia()
        }
    }

    fun scanMedia() {
        if (_isScanning.value) return
        _isScanning.value = true
        viewModelScope.launch {
            // Get current settings
            val folders = customFolders.value
            
            musicRepository.getSongs(folders).collectLatest { songList ->
                // Store raw result
                _allSongs.value = songList
                
                updateVisibleSongs()
                
                _isScanning.value = false
                tryRestoreState() // Try restoring after library load
                checkDailyRefresh() // Check if we need to generate daily recommendations
            }
        }
    }

    // [UPDATED] Public for Lifecycle Hook
    fun checkDailyRefresh() {
        val currentState = _recommendationState.value
        val now = System.currentTimeMillis()
        
        // Simple efficient check: Is it time yet?
        if (now >= currentState.nextRefreshTime) {
            refreshDailyRecommendations(force = false)
        } else {
            // Re-schedule if needed (e.g. app was killed and restarted before time)
            scheduleNextAutoRefresh(currentState.nextRefreshTime)
        }
    }

    fun toggleFavorite(songId: Long) {
        val current = _favoriteIds.value
        val newSet = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
        _favoriteIds.value = newSet
        // Persist
        prefs.edit().putStringSet("favorite_ids", newSet.map { it.toString() }.toSet()).apply()
    }

    // Selection State
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) current - id else current + id
        
        val isEmpty = _selectedIds.value.isEmpty()
        if (isEmpty && _isSelectionMode.value) {
            _isSelectionMode.value = false
        } else if (!isEmpty && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }
    
    fun selectAll(ids: List<Long>) {
        if (_selectedIds.value.size == ids.size) {
            clearSelection() // Auto-exit when deselecting all
        } else {
            _selectedIds.value = ids.toSet()
        }
    }
    
    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    // User Playlist Management
    fun createUserPlaylist(name: String) {
        if (_userPlaylists.value.size >= 20) {
            return 
        }
        val newPlaylist = Playlist(name = name)
        _userPlaylists.value = _userPlaylists.value + newPlaylist
        savePlaylists()
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val currentPlaylists = _userPlaylists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = currentPlaylists[index]
            // Allow duplicates as requested
            playlist.songs.add(song)
            // Force emit
            _userPlaylists.value = ArrayList(currentPlaylists) 
            savePlaylists()
        }
    }
    
    fun addToNext(songs: List<Song>) {
        val controller = mediaController ?: return
        if (songs.isEmpty()) return
        
        val currentIndex = controller.currentMediaItemIndex
        val nextIndex = if (currentIndex == -1) 0 else currentIndex + 1
        
        val mediaItems = songs.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.contentUri) 
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setArtworkUri(item.albumArtUri)
                        .build()
                )
                .build()
        }
        
        controller.addMediaItems(nextIndex, mediaItems)
        
        // Optional: Show toast or feedback "Added to play next"
        // But VM shouldn't do UI.
    }
    
    private fun savePlaylists() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            playlistRepository.savePlaylists(_userPlaylists.value)
        }
    }
    
    // Helper to find most played song in a playlist for UI Cover
    fun getMostPlayedSong(playlist: Playlist): Song? {
        if (playlist.songs.isEmpty()) return null
        val counts = _playCounts.value
        return playlist.songs.maxByOrNull { counts[it.id] ?: 0 }
    }
    
    // Sort songs in a playlist manually
    fun reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        val currentPlaylists = _userPlaylists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = currentPlaylists[index]
            val songs = playlist.songs
            
            if (fromIndex in songs.indices && toIndex in songs.indices) {
                val song = songs.removeAt(fromIndex)
                songs.add(toIndex, song)
                
                // Force Update
                 _userPlaylists.value = ArrayList(currentPlaylists) 
                savePlaylists()
            }
        }
    }

    // Remove song from playlist
    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
         val currentPlaylists = _userPlaylists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = currentPlaylists[index]
            playlist.songs.removeAll { it.id == songId }
            
             // Force Update
             _userPlaylists.value = ArrayList(currentPlaylists) 
            savePlaylists()
        }
    }

    // Overload for Convenience
    fun removeSongFromPlaylist(playlist: Playlist, song: Song) {
        removeSongFromPlaylist(playlist.id, song.id)
    }

    // Helper Toast
    fun showToast(message: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Remove from Queue (Current Playback)
    fun removeFromQueue(index: Int) {
         mediaController?.removeMediaItem(index)
         // Listener will update _currentQueue automatically
         
         // If we removed the current playing song, player logic handles it (skips to next or stops).
    }

    // Playback Controls
    // Playback Controls
    // Playlist State Separation
    // 1. User Playlists: Created by user, Max 20
    private val _userPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<Playlist>> = _userPlaylists.asStateFlow()

    // 2. Playback Contexts: Auto-generated history, Max 3
    private val _playbackContexts = MutableStateFlow<List<Playlist>>(emptyList())
    // Not necessarily exposed to UI unless for a "History" screen, but keeping it internal for now if unused.

    // Play Counts Map: songId -> count
    private val _playCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts.asStateFlow() // [NEW] Expose for UI
    
    // Playback Controls
    fun playSong(song: Song, contextList: List<Song> = emptyList(), listName: String = "Queue") {
        val controller = mediaController ?: return
        
        // Increment Play Count
        val currentCounts = _playCounts.value.toMutableMap()
        currentCounts[song.id] = (currentCounts[song.id] ?: 0) + 1
        _playCounts.value = currentCounts
        
        // Persist counts
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            playlistRepository.savePlayCounts(currentCounts)
        }

        // 1. Handle Playback Context (Auto History)
        val targetList = if (contextList.isNotEmpty()) contextList else listOf(song)
        
        // [NEW] Track Recommendation Context
        currentRecommendationContext = (listName == "每日推荐" || listName == "Daily Recommend")
        
        if (contextList.isNotEmpty()) {
            // Create Context Playlist
            val contextPlaylist = Playlist(
                id = java.util.UUID.randomUUID().toString(),
                name = listName,
                songs = ArrayList(contextList) // Copy
            )
            
            // Update Playback Contexts Limit: Add new, Keep only top 3
            val currentContexts = _playbackContexts.value
            _playbackContexts.value = (listOf(contextPlaylist) + currentContexts).take(3)
        }
        
        // ... (MediaItem preparation continues below)

        // 2. Prepare MediaItems for Controller
        val mediaItems = targetList.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.contentUri) 
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setArtworkUri(item.albumArtUri)
                        .build()
                )
                .build()
        }
            
        // 3. Set Queue and Play
        controller.setMediaItems(mediaItems)
        
        // 4. Seek to the specific song
        val startIndex = targetList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        controller.seekTo(startIndex, 0)
        
        controller.prepare()
        controller.play()
        
        _currentSong.value = song
        loadLyrics(song) // Ensure lyrics are loaded immediately when playing a song
        
        // [SMART SHUFFLE FIX] Enforce Shuffle State for New List
        if (_isShuffleEnabled.value) {
            // Re-apply shuffle to valid new queue
            controller.shuffleModeEnabled = true 
            // Ensure we are in Repeat All to prevent stopping
            controller.repeatMode = Player.REPEAT_MODE_ALL 
        } else {
             controller.shuffleModeEnabled = false
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        // Optimistic update to prevent slider jump
        _progress.value = positionMs.toFloat()
    }

    fun seekAndPlay(positionMs: Long) {
        mediaController?.let {
            it.seekTo(positionMs)
            if (!it.isPlaying) {
                it.play()
            }
        }
        _progress.value = positionMs.toFloat()
    }

    fun skipNext() {
        if (mediaController?.hasNextMediaItem() == true) {
            mediaController?.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (mediaController?.hasPreviousMediaItem() == true) {
            mediaController?.seekToPreviousMediaItem()
        }
    }
    
    // Repeat Mode
    // Repeat Mode: Loaded from SharedPreferences (No hardcoded default)
    private val _repeatMode = MutableStateFlow<Int>(Player.REPEAT_MODE_ALL) // Temporary initial value, will be overridden in init
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    fun toggleRepeatMode(forceMode: Int? = null) {
        mediaController?.let {
            val nextMode = forceMode ?: when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL // Start with All
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL // Loop back to All (No Off)
                else -> Player.REPEAT_MODE_ALL
            }
            it.repeatMode = nextMode
            _repeatMode.value = nextMode
            
            // Save repeatMode to SharedPreferences
            prefs.edit().putInt("repeat_mode", nextMode).apply()
            
            // Sync Shuffle state if Repeat All is forced (usually disables shuffle logic visually)
            // But here we rely on isShuffleEnabled flag.
            
            // Reset loop counts on manual toggle
            if (forceMode == null) {
                 _targetLoopCount.value = 0
                 _remainingLoopCount.value = 0
            }
        }
    }
    
    // Cycle: Shuffle -> Repeat All -> Repeat One -> Shuffle (Loop)
    // "Repeat Off" is removed from the cycle.
    // Cycle: Shuffle -> Repeat All -> Repeat One -> Shuffle (Loop)
    // Mode Mapping:
    // Shuffle: Shuffle=True, Repeat=OFF (To detect end for Smart Shuffle)
    // Repeat All: Shuffle=False, Repeat=ALL
    // Repeat One: Shuffle=False, Repeat=ONE
    fun cyclePlayMode() {
        val controller = mediaController ?: return
        
        // 1. If Loop Count is active, clear it first
        if (_targetLoopCount.value > 0) {
            _targetLoopCount.value = 0
            _remainingLoopCount.value = 0
            // Reset to Shuffle (Cycle start)
            controller.repeatMode = Player.REPEAT_MODE_OFF // Smart Shuffle uses OFF
            controller.shuffleModeEnabled = true
            _repeatMode.value = Player.REPEAT_MODE_OFF // UI Mapping needed?
            _isShuffleEnabled.value = true
            prefs.edit().putInt("repeat_mode", Player.REPEAT_MODE_OFF).apply()
            prefs.edit().putBoolean("shuffle_enabled", true).apply()
            return
        }

        val isShuffle = _isShuffleEnabled.value
        val repeat = _repeatMode.value
        // Logic:
        // 1. If currently Shuffle (regardless of repeat), go to Repeat All
        // 2. If Repeat All, go to Repeat One
        // 3. If Repeat One, go to Shuffle
        
        if (isShuffle) {
            // Shuffle -> Repeat All
            controller.shuffleModeEnabled = false
            controller.repeatMode = Player.REPEAT_MODE_ALL
            _isShuffleEnabled.value = false
            _repeatMode.value = Player.REPEAT_MODE_ALL
            prefs.edit().putInt("repeat_mode", Player.REPEAT_MODE_ALL).apply()
            prefs.edit().putBoolean("shuffle_enabled", false).apply()
        } else {
             when (repeat) {
                 Player.REPEAT_MODE_ALL -> {
                     // Repeat All -> Repeat One
                     controller.repeatMode = Player.REPEAT_MODE_ONE
                     _repeatMode.value = Player.REPEAT_MODE_ONE
                     prefs.edit().putInt("repeat_mode", Player.REPEAT_MODE_ONE).apply()
                 }
                 Player.REPEAT_MODE_ONE -> {
                     // Repeat One -> Shuffle
                     // [SMART SHUFFLE] Use REPEAT_MODE_ALL for infinite playback
                     controller.repeatMode = Player.REPEAT_MODE_ALL
                     controller.shuffleModeEnabled = true 
                     
                     _repeatMode.value = Player.REPEAT_MODE_ALL // UI shows Loop All + Shuffle
                     _isShuffleEnabled.value = true
                     prefs.edit().putInt("repeat_mode", Player.REPEAT_MODE_ALL).apply()
                     prefs.edit().putBoolean("shuffle_enabled", true).apply()
                 }
                 else -> { 
                     // Off/Default -> Shuffle (Entry)
                     controller.repeatMode = Player.REPEAT_MODE_ALL
                     controller.shuffleModeEnabled = true
                     _repeatMode.value = Player.REPEAT_MODE_ALL
                     _isShuffleEnabled.value = true
                     prefs.edit().putInt("repeat_mode", Player.REPEAT_MODE_ALL).apply()
                     prefs.edit().putBoolean("shuffle_enabled", true).apply()
                 }
             }
        }
    }
    

    
    fun setShuffleMode(enabled: Boolean) {
        _isShuffleEnabled.value = enabled
        // Save shuffle state
        prefs.edit().putBoolean("shuffle_enabled", enabled).apply()
        
        val controller = mediaController ?: return
        
        if (enabled) {
            // [SMART SHUFFLE FIX] 
            // Do NOT physically shuffle the queue (Fisher-Yates on list).
            // User wants Visual Order (Sorted) + Random Playback (Internal Shuffle).
            
            // 1. Enable Player Internal Shuffle
            controller.shuffleModeEnabled = true
            
            // 2. Set Repeat to ALL (for Infinite Playback)
            controller.repeatMode = Player.REPEAT_MODE_ALL
            _repeatMode.value = Player.REPEAT_MODE_ALL
            prefs.edit().putInt("repeat_mode", Player.REPEAT_MODE_ALL).apply()
            
            // 3. Ensure Queue reflects "Visual Order" (Timeline order)
            // If the queue was already sorted, we leave it alone.
            // If updateCurrentQueueFromController() is called, it will pull timeline order.
            
        } else {
            // Disable Shuffle
            controller.shuffleModeEnabled = false
            // Restore Repeat Mode via cycle or default to ALL/OFF?
            // Usually disabling shuffle reverts to sequential play.
        }
    }



    // Persistence Logic
    private var isRestored = false

    private fun savePlaybackState() {
        val controller = mediaController ?: return
        val currentSongId = _currentSong.value?.id ?: return
        val currentPosition = controller.currentPosition
        val currentQueueIds = _currentQueue.value.map { it.id }
        
        if (currentQueueIds.isEmpty()) return

        val data = com.vagueplayer.music.data.repository.PlaylistRepository.LastSessionData(
            lastPlayedSongId = currentSongId,
            lastPositionMs = currentPosition,
            lastPlaylistIds = currentQueueIds,
            shuffleMode = _isShuffleEnabled.value,
            repeatMode = _repeatMode.value
        )
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            playlistRepository.savePlayCounts(_playCounts.value) // Save counts too
            playlistRepository.saveLastSession(data)
        }
    }
    
    private fun tryRestoreState() {
        if (isRestored) return
        val controller = mediaController ?: return
        val allSongs = _songs.value
        
        if (allSongs.isEmpty()) return // Wait for library
        
        // If Service is already playing/has content, DO NOT overwrite
        if (controller.mediaItemCount > 0) {
            isRestored = true
            return
        }
        
        viewModelScope.launch {
            val lastSession = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                playlistRepository.loadLastSession()
            } ?: return@launch
            
            // Reconstruct Queue
            val queue = lastSession.lastPlaylistIds.mapNotNull { id ->
                allSongs.find { it.id == id }
            }
            
            if (queue.isNotEmpty()) {
                val mediaItems = queue.map { item ->
                    MediaItem.Builder()
                        .setMediaId(item.id.toString())
                        .setUri(item.contentUri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(item.title)
                                .setArtist(item.artist)
                                .setArtworkUri(item.albumArtUri)
                                .build()
                        )
                        .build()
                }
                
                controller.setMediaItems(mediaItems)
                
                // Find index
                val index = queue.indexOfFirst { it.id == lastSession.lastPlayedSongId }.coerceAtLeast(0)
                
                // Seek to index and position
                controller.seekTo(index, lastSession.lastPositionMs)
                controller.prepare()
                controller.pause() // Ensure it starts paused
                
                // Restore Repeat/Shuffle Mode
                _isShuffleEnabled.value = lastSession.shuffleMode
                
                // Apply Repeat Mode to Controller and UI
                controller.repeatMode = lastSession.repeatMode
                _repeatMode.value = lastSession.repeatMode
                
                // Initialize UI State manually since no transition might happen until play
                _currentQueue.value = queue
                val song = queue[index]
                _currentSong.value = song
                loadLyrics(song)
                _progress.value = lastSession.lastPositionMs.toFloat()
                _duration.value = song.duration // Estimate or 0
                
                isRestored = true
                Log.d("AudioViewModel", "Restored Last Session: Song=${song.title} Pos=${lastSession.lastPositionMs}")
            }
        }
    }

    init {
        // Load Persisted Settings
        _isSidebarOnLeft.value = prefs.getBoolean("sidebar_on_left", false)
        // Load saved repeatMode (Default to REPEAT_MODE_ALL if not saved)
        _repeatMode.value = prefs.getInt("repeat_mode", Player.REPEAT_MODE_ALL)
        // Load saved shuffleMode (Default to false)
        _isShuffleEnabled.value = prefs.getBoolean("shuffle_enabled", false)
        // REMOVED: loadGaplessSetting()
        loadFavorites()
        loadSongStatistics() // Load song statistics for recommendation engine 
        loadRecommendationState() // Load recommendation state
                
        // Load Repository Data
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _userPlaylists.value = playlistRepository.loadPlaylists()
            _playCounts.value = playlistRepository.loadPlayCounts()
        }
        
        initializeMediaController()
        
        
        // Auto-Scan if folders are configured
        viewModelScope.launch {
             customFolders.collectLatest { folders ->
                 if (folders.isNotEmpty() && !_isScanning.value && _songs.value.isEmpty()) {
                     Log.d("AudioViewModel", "Auto-scanning on launch with ${folders.size} folders")
                     scanMedia()
                 }
             }
        }

        
        // Start polling for progress
        viewModelScope.launch {
            var lastPolledPosition = 0L
            while (true) {
                if (_isPlaying.value && mediaController != null) {
                    val currentPos = mediaController?.currentPosition ?: 0L
                    val currentDur = _duration.value
                    
                    _progress.value = currentPos.toFloat()
                    updateLyricIndex(currentPos)
                    // Track playback progress for analytics
                    playbackAnalyzer.onPlaybackProgress(currentPos)
                    
                    // POLLING LOOP DETECTION
                    if (_repeatMode.value == Player.REPEAT_MODE_ONE && _targetLoopCount.value > 0) {
                        if (currentDur > 0 && lastPolledPosition > (currentDur - 2000) && currentPos < 2000) {
                            if (_remainingLoopCount.value > 0) {
                                _remainingLoopCount.value -= 1
                                Log.d("AudioViewModel", "Polling Loop Detected. Remaining: ${_remainingLoopCount.value}")
                                
                                if (_remainingLoopCount.value <= 0) {
                                    Log.d("AudioViewModel", "Polling Loop Finished. Next.")
                                    _targetLoopCount.value = 0
                                    toggleRepeatMode(Player.REPEAT_MODE_ALL) 
                                     if (mediaController?.hasNextMediaItem() == true) {
                                         mediaController?.seekToNextMediaItem()
                                     } else {
                                         mediaController?.seekTo(0, 0)
                                     }
                                }
                            }
                        }
                    }
                    
                    lastPolledPosition = currentPos
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        savePlaybackState()
        controllerFuture?.let {
            try {
                MediaController.releaseFuture(it)
            } catch (e: Exception) {
                // Ignore "Service not registered" or other release errors.
                // If it wasn't registered, our job (releasing) is effectively done or irrelevant.
                Log.w("AudioViewModel", "Error releasing MediaController future: ${e.message}")
            }
        }
        mediaController = null
    }
}
