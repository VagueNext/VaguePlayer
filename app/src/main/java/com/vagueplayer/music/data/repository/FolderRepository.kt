package com.vagueplayer.music.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vagueplayer.music.data.model.MusicFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "music_folders")

class FolderRepository(private val context: Context) {

    private val USE_MEDIA_STORE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("use_android_media_store")
    
    val useAndroidMediaStore: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_MEDIA_STORE_KEY] ?: true // Default to true
        }
        
    private val FOLDERS_KEY = stringSetPreferencesKey("custom_music_folders")

    val customFolders: Flow<List<MusicFolder>> = context.dataStore.data
        .map { preferences ->
            val folderStrings = preferences[FOLDERS_KEY] ?: emptySet()
            folderStrings.map { folderString ->
                val parts = folderString.split("|")
                if (parts.size >= 2) {
                    MusicFolder(Uri.parse(parts[0]), parts[1], parts.getOrElse(2) { "" })
                } else {
                    MusicFolder(Uri.parse(parts[0]), "Unknown")
                }
            }
        }

    suspend fun setUseAndroidMediaStore(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_MEDIA_STORE_KEY] = enabled
        }
    }

    suspend fun addFolder(musicFolder: MusicFolder) {
        context.dataStore.edit { preferences ->
            val currentFolders = preferences[FOLDERS_KEY] ?: emptySet()
            val newFolderString = "${musicFolder.uri}|${musicFolder.displayName}|${musicFolder.fullPath}"
            preferences[FOLDERS_KEY] = currentFolders + newFolderString
        }
    }

    suspend fun removeFolder(uri: Uri) {
        context.dataStore.edit { preferences ->
            val currentFolders = preferences[FOLDERS_KEY] ?: emptySet()
            val folderToRemove = currentFolders.find { it.startsWith(uri.toString()) }
            if (folderToRemove != null) {
                preferences[FOLDERS_KEY] = currentFolders - folderToRemove
            }
        }
    }
}
