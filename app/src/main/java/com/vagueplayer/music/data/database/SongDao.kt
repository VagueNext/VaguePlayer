package com.vagueplayer.music.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    // Sync Logic
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongsMetaEntity>)

    @Query("DELETE FROM songs_meta")
    suspend fun clearSongs()

    @Query("DELETE FROM lyrics_fts")
    suspend fun clearLyrics()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: List<LyricsFtsEntity>)
    
    // Sync Transaction
    @Transaction
    suspend fun syncLibrary(songs: List<SongsMetaEntity>) {
        // Differential update is better, but allow full replace for simplicity of "Scan"
        // Ideally we don't clear, just upsert.
        insertSongs(songs)
    }

    // TRACK 1: Metadata Search (Instant)
    // Ordered by: Artist Match -> Title Match -> Album Match
    @Query("""
        SELECT * FROM songs_meta 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%'
        ORDER BY 
           CASE WHEN artist LIKE :query || '%' THEN 1 
                WHEN title LIKE :query || '%' THEN 2 
                ELSE 3 END ASC
        LIMIT 50
    """)
    suspend fun searchMeta(query: String): List<SongsMetaEntity>

    // TRACK 2: Lyrics FTS Search (Delayed)
    // Join with Meta to return full objects
    @Transaction
    @Query("""
        SELECT songs_meta.* FROM songs_meta
        JOIN lyrics_fts ON songs_meta.id = lyrics_fts.songId
        WHERE lyrics_fts MATCH :query
        LIMIT 20
    """)
    suspend fun searchLyrics(query: String): List<SongsMetaEntity>

    // Helper: Get All Songs for initial load if needed (repository uses this)
    @Query("SELECT * FROM songs_meta ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongsMetaEntity>>
}
