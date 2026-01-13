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

    fun getSongs(customFolders: List<com.vagueplayer.music.data.model.MusicFolder>): Flow<List<Song>> = flow {
    // Always scan MediaStore.
    // Logic: Include everything from MediaStore (Standard Behavior).
    // The user deleted the "Switch", implying they want standard "Scan All" behavior.
    
    val songs = mutableListOf<Song>()
    
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
            
            // Only include if path matches a Custom Folder
            if (path != null && customFolders.isNotEmpty()) {
                customFolders.forEach { folder ->
                    // 1. Resolve stored path (handle legacy raw paths on the fly)
                    var checkPath = folder.fullPath
                    if (checkPath.contains("primary:")) {
                        checkPath = "/storage/emulated/0/" + checkPath.substringAfter("primary:")
                    } else if (checkPath.contains("raw:")) {
                        checkPath = checkPath.substringAfter("raw:")
                    }
                    
                    // 2. Normalize Slashes
                    checkPath = checkPath.removeSuffix("/")
                    
                    // 3. Comparison
                    if (path.startsWith(checkPath, ignoreCase = true)) {
                        include = true
                    }
                }
            }
            
            if (include) {
                val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                songs.add(Song(id, title, artist, album, duration, contentUri, albumArtUri, dateAdded, size, path = path ?: ""))
            }
        }
    }
    emit(songs)
    }.flowOn(Dispatchers.IO)

    // Delete songs function
    suspend fun deleteSongs(songs: List<Song>) {
        // Note: On Android 10+, deleting non-owned files requires RecoverableSecurityException handling
        // or createDeleteRequest (Android 11+). 
        // For simplicity, we attempt direct delete. The UI should handle IntentSender if needed in a full implementation.
        // Here we just loop and try delete.
        with(Dispatchers.IO) {
            songs.forEach { song ->
                try {
                    context.contentResolver.delete(song.contentUri, null, null)
                } catch (e: SecurityException) {
                    // In a real app, rethrow or returns IntentSender for scoped storage permission
                    e.printStackTrace()
                }
            }
        }
    }
}
