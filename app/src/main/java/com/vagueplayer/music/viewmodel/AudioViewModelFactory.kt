package com.vagueplayer.music.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vagueplayer.music.data.repository.FolderRepository
import com.vagueplayer.music.data.repository.MusicRepository
import com.vagueplayer.music.data.repository.PlaylistRepository

class AudioViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioViewModel::class.java)) {
            val musicRepository = MusicRepository(context)
            val folderRepository = FolderRepository(context)
            val playlistRepository = PlaylistRepository(context)
            @Suppress("UNCHECKED_CAST")
            return AudioViewModel(context, musicRepository, folderRepository, playlistRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
