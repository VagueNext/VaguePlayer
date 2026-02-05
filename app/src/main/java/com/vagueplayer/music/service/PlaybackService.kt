package com.vagueplayer.music.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture // Fix Unresolved reference

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        
        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false // handleAudioFocus: Default FALSE to allow mixing (Concurrent Playback)
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        // Error Handling: Auto-skip malformed songs
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlaybackService", "Critical Player Error: ${error.errorCodeName}", error)
                
                // If the file is malformed (ParserException) or unplayable, try to skip
                if (player.hasNextMediaItem()) {
                    android.util.Log.w("PlaybackService", "Skipping unplayable media item...")
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                } else {
                    android.util.Log.w("PlaybackService", "Cannot skip error (No next item).")
                    // Stop to prevent infinite retry loops on single bad file
                    player.stop() 
                }
            }
        })
        
        // REMOVED: Default repeatMode (Now controlled by ViewModel state restoration)

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .build()
    }
    
    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<androidx.media3.session.SessionResult> {
            if (customCommand.customAction == "SET_MIX_AUDIO") {
                val isMixEnabled = args.getBoolean("enabled", true)
                // If Mix is ALLOWED (true), then handleAudioFocus must be FALSE (Don't fight for focus).
                // If Mix is DISABLED (false), then handleAudioFocus must be TRUE (Exclusive mode).
                val handleFocus = !isMixEnabled
                
                player.setAudioAttributes(player.audioAttributes, handleFocus)
                
                return com.google.common.util.concurrent.Futures.immediateFuture(
                    androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                )
            }
            // REMOVED: SET_GAPLESS command (gapless playback is default in ExoPlayer)
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
    
    // The service lifecycle is managed by MediaSessionService
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    
    // Starting in Android 12 (API 31), we can't start foreground services from background
    // effortlessly, but MediaSessionService handles simple cases.
    // For more complex notification handling, we'll rely on Media3's default notification provider first.
}
