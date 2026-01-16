package com.vagueplayer.music.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel
import com.vagueplayer.music.ui.components.GlassInputDialog
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: AudioViewModel,
    onDismissRequest: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Live Data Observation
    val currentPlaylist by produceState(initialValue = playlist, key1 = playlist.id, key2 = viewModel.userPlaylists) {
        viewModel.userPlaylists.collect { list ->
            val updated = list.find { it.id == playlist.id }
            if (updated != null) value = updated
        }
    }
    
    val coverSong = viewModel.getMostPlayedSong(currentPlaylist) 
    
    // Dialog State
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // 1. Root Surface: Full Screen, Opaque Background to prevent transparency glitches
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // 2. Core Layout: Column + VerticalScroll (Instantly calculates header position)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) 
        ) {
            
            // === Top: Immersive Header Image Area ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp) // Pro-level height
            ) {
                val displaySong = coverSong ?: currentPlaylist.songs.firstOrNull()
                
                // A. Cover Image
                if (displaySong != null) {
                    AsyncImage(
                        model = displaySong.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop, // Must match Source (Crop)
                        modifier = Modifier
                            .fillMaxSize()
                            // 🔥 CRITICAL FIX 1: Elevate Z-Index to prevent being covered by background during entering transition
                            .zIndex(10f) 
                            .sharedElement(
                                state = rememberSharedContentState(key = "playlist_cover_${currentPlaylist.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                // 🔥 CRITICAL FIX 2: Force animated size measurement
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                                boundsTransform = { _, _ ->
                                    spring(dampingRatio = 0.8f, stiffness = 380f)
                                }
                            )
                            .background(Color.LightGray)
                    )
                } else {
                     Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray)
                    )
                }
                
                // B. Gradient Overlay (Bottom-up black gradient for text)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // C. Title (Overlay on image)
                Text(
                    text = currentPlaylist.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .sharedElement(
                            state = rememberSharedContentState(key = "playlist_title_${currentPlaylist.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) }
                        )
                        .clickable { 
                             renameText = currentPlaylist.name
                             showRenameDialog = true
                        }
                )

                // D. Close Button (Top-Left, Status Bar Padded)
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding() // Only pad the button
                        .padding(16.dp)
                ) {
                    // Semi-transparent circle background
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.3f), 
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            // === Bottom: Content Area ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Play All Button
                Button(
                    onClick = {
                        if (currentPlaylist.songs.isNotEmpty()) {
                            viewModel.playSong(currentPlaylist.songs.first(), currentPlaylist.songs, currentPlaylist.name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.vagueplayer.music.ui.theme.AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("全部播放 (${currentPlaylist.songs.size})")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Song List
                if (currentPlaylist.songs.isEmpty()) {
                    Text("暂无歌曲", color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    currentPlaylist.songs.forEachIndexed { index, song ->
                        Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clickable { 
                                     viewModel.playSong(song, currentPlaylist.songs, currentPlaylist.name) 
                                 }
                                 .padding(vertical = 12.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(
                                 "${index + 1}", 
                                 color = Color.Gray, 
                                 modifier = Modifier.width(30.dp),
                                 style = MaterialTheme.typography.bodyMedium
                             )
                             
                             AsyncImage(
                                 model = song.albumArtUri,
                                 contentDescription = null,
                                 contentScale = ContentScale.Crop,
                                 modifier = Modifier
                                     .size(50.dp)
                                     .clip(RoundedCornerShape(8.dp))
                                     .background(Color.LightGray)
                             )
                             
                             Spacer(modifier = Modifier.width(16.dp))
                             
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(
                                     song.title, 
                                     style = MaterialTheme.typography.bodyLarge,
                                     fontWeight = FontWeight.Medium,
                                     maxLines = 1,
                                     overflow = TextOverflow.Ellipsis
                                 )
                                 Text(
                                     song.artist, 
                                     style = MaterialTheme.typography.bodySmall, 
                                     color = Color.Gray,
                                     maxLines = 1,
                                     overflow = TextOverflow.Ellipsis
                                 )
                             }
                        }
                    }
                }
                
                // Bottom Spacing for Navigation Bar + MiniPlayer
                Spacer(modifier = Modifier.navigationBarsPadding())
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    
    // Rename Dialog
    if (showRenameDialog) {
        GlassInputDialog(
            hazeState = null,
            title = "重命名歌单",
            initialValue = renameText,
            icon = Icons.Default.Edit,
            onConfirm = { newName ->
                if (newName.isNotBlank()) viewModel.renamePlaylist(currentPlaylist.id, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}
