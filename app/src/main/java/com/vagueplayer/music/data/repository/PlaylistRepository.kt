package com.vagueplayer.music.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.data.model.Song
import java.io.File
import java.lang.reflect.Type

class PlaylistRepository(private val context: Context) {
    
    private val playlistFile = File(context.filesDir, "user_playlists.json")
    private val playCountsFile = File(context.filesDir, "play_counts.json")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

    // --- Playlists ---
    
    fun savePlaylists(playlists: List<Playlist>) {
        try {
            val json = gson.toJson(playlists)
            playlistFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadPlaylists(): List<Playlist> {
        if (!playlistFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson(playlistFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // --- Play Counts ---
    
    fun savePlayCounts(counts: Map<Long, Int>) {
        try {
            val json = gson.toJson(counts)
            playCountsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadPlayCounts(): Map<Long, Int> {
        if (!playCountsFile.exists()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<Long, Int>>() {}.type
            gson.fromJson(playCountsFile.readText(), type) ?: emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    // --- Last Session Persistence ---

    data class LastSessionData(
        val lastPlayedSongId: Long,
        val lastPositionMs: Long,
        val lastPlaylistIds: List<Long>,
        val shuffleMode: Boolean = false,
        val repeatMode: Int = 0 // Player.REPEAT_MODE_OFF
    )

    private val sessionFile = File(context.filesDir, "last_session.json")

    fun saveLastSession(data: LastSessionData) {
        try {
            val json = gson.toJson(data)
            sessionFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLastSession(): LastSessionData? {
        if (!sessionFile.exists()) return null
        return try {
            gson.fromJson(sessionFile.readText(), LastSessionData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Type Adapter for Uri ---
    private class UriTypeAdapter : JsonSerializer<Uri>, JsonDeserializer<Uri> {
        override fun serialize(src: Uri?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src.toString())
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Uri {
            return Uri.parse(json?.asString)
        }
    }
}
