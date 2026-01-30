package com.vagueplayer.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.vagueplayer.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MusicRepository(private val context: Context) {

    // Database Initialization
    private val database by lazy { com.vagueplayer.music.data.database.AppDatabase.getDatabase(context) }
    private val songDao by lazy { database.songDao() }

    // --- SEARCH API (Async Two-Tier) ---
    
    // Track 1: Metadata (Fast)
    suspend fun searchMeta(query: String): List<Song> {
        return songDao.searchMeta(query).map { it.toModel() }
    }

    // Track 2: Lyrics (FTS4)
    suspend fun searchLyrics(query: String): List<Song> {
        return songDao.searchLyrics(query).map { it.toModel() }
    }

    // --- MAPPERS ---
    private fun Song.toEntity(): com.vagueplayer.music.data.database.SongsMetaEntity {
        return com.vagueplayer.music.data.database.SongsMetaEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            contentUri = contentUri.toString(),
            albumArtUri = albumArtUri?.toString(),
            dateAdded = dateAdded,
            size = size,
            path = path
        )
    }

    private fun com.vagueplayer.music.data.database.SongsMetaEntity.toModel(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            contentUri = Uri.parse(contentUri),
            albumArtUri = if (albumArtUri != null) Uri.parse(albumArtUri) else null,
            dateAdded = dateAdded,
            size = size,
            path = path
        )
    }

    fun getSongs(customFolders: List<com.vagueplayer.music.data.model.MusicFolder>): Flow<List<Song>> = flow {
        // ... (Scanning Logic Same as Before) ...
        val songs = mutableListOf<Song>()
        val entities = mutableListOf<com.vagueplayer.music.data.database.SongsMetaEntity>()
        val lyricsEntities = mutableListOf<com.vagueplayer.music.data.database.LyricsFtsEntity>()
        
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown"
                val album = cursor.getString(albumColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val path = cursor.getString(dataColumn)
                val size = cursor.getLong(sizeColumn)

                // STRICT WHITELIST LOGIC
                var include = false
                if (path != null && customFolders.isNotEmpty()) {
                    customFolders.forEach { folder ->
                        var checkPath = folder.fullPath
                        if (checkPath.contains("primary:")) {
                            checkPath = "/storage/emulated/0/" + checkPath.substringAfter("primary:")
                        } else if (checkPath.contains("raw:")) {
                            checkPath = checkPath.substringAfter("raw:")
                        }
                        checkPath = checkPath.removeSuffix("/")
                        if (path.startsWith(checkPath, ignoreCase = true)) {
                            include = true
                        }
                    }
                }
                
                if (include) {
                    val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id) // [FIX] ID
                    val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                    val song = Song(id, title, artist, album, duration, contentUri, albumArtUri, dateAdded, size, path = path ?: "")
                    songs.add(song)
                    
                    // Prepare for DB Sync
                    entities.add(song.toEntity())
                }
            }
        }
        
        // 1. Emit Results to UI immediately
        emit(songs)
        
        // 2. Sync to Database (Background)
        try {
            songDao.syncLibrary(entities)
            
            // 3. Index Lyrics (Lazy Indexing) if needed
            // Only try to parse if we have paths. For now, simple scan:
            // This could be heavy, so maybe dispatch separately? 
            // Repository flow runs on IO, so blocking here delays... nothing? 
            // Ah, the ViewModel collects this flow. If we block here, the collection finishes late?
            // "emit" happened already. So this runs AFTER first emission? 
            // In a cold flow, yes code continues.
            
            val lyricsList = mutableListOf<com.vagueplayer.music.data.database.LyricsFtsEntity>()
            entities.forEach { entity ->
                if (entity.path.isNotEmpty()) {
                    // Quick check for matching .lrc file?
                    // Or parse everything? Using LrcParser.
                    // This assumes we have permission to read the file alongside the media.
                    try {
                        // We use the URI to load via LrcParser which uses ContentResolver or File if path exists
                        // LrcParser.loadLyricsForSong uses URI.
                        // But LrcParser usually runs on UI demand.
                        // Let's rely on entity.path to find .lrc
                        // Simplified: Check if adjacent .lrc exists
                        val lrcPath = entity.path.substringBeforeLast(".") + ".lrc"
                        val lrcFile = java.io.File(lrcPath)
                        if (lrcFile.exists()) {
                            val content = lrcFile.readText()
                            if (content.isNotBlank()) {
                                lyricsList.add(com.vagueplayer.music.data.database.LyricsFtsEntity(entity.id, content))
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore read errors during indexing
                    }
                }
            }
            if (lyricsList.isNotEmpty()) {
                songDao.insertLyrics(lyricsList)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
    }.flowOn(Dispatchers.IO)

    // Delete songs function
    suspend fun deleteSongs(songs: List<Song>) {
        with(Dispatchers.IO) {
            songs.forEach { song ->
                try {
                    context.contentResolver.delete(song.contentUri, null, null)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
            // Sync Deletion to DB
            // Ideally we re-scan or delete by ID.
            // For now, next scan fixes it.
        }
    }
}
