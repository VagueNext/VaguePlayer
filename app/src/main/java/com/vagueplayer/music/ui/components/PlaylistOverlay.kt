package com.vagueplayer.music.ui.components

import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.viewmodel.AudioViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow

@Composable
fun GlassPlaylistOverlay(
    viewModel: AudioViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    blurRadius: androidx.compose.ui.unit.Dp = LiquidGlassDefaults.BlurRadius,
    tint: androidx.compose.ui.graphics.Color = LiquidGlassDefaults.Tint,
    edgeWidth: Float = LiquidGlassDefaults.EdgeWidth,
    addToPlaylistMode: Boolean = false,
    songsToAdd: List<com.vagueplayer.music.data.model.Song> = emptyList(),
    customListMode: Boolean = false,
    customSongs: List<com.vagueplayer.music.data.model.Song> = emptyList(),
    customTitle: String? = null,
    // hazeState removed to force AGSL
) {
    val currentQueue by viewModel.currentQueue.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val playlists by viewModel.userPlaylists.collectAsState()

    // Predictive Back
    var backProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    
    androidx.activity.compose.PredictiveBackHandler(enabled = isVisible) { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
        } catch (e: java.util.concurrent.CancellationException) {
            backProgress = 0f
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // Exactly lower half of screen
                     .simpleGlass(
                         cornerRadius = 24.dp,
                         distortionStrength = 60f, // Increased for visibility
                         edgeWidth = 40f // Increased for visibility
                     )
                     .background(Color.White.copy(alpha = 0.4f)) // Ensure visible tint
                    .clickable(enabled = false) {}
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text(
                             text = if (addToPlaylistMode) "添加到歌单" else if (customListMode) (customTitle ?: "歌曲列表") else "播放列表 (${currentQueue.size})",
                             color = Color.Black,
                             fontSize = 20.sp,
                             fontWeight = FontWeight.Bold
                         )
                    }

                    LazyColumn {
                        if (addToPlaylistMode) {
                            itemsIndexed(playlists) { index, playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable { 
                                            songsToAdd.forEach { song ->
                                                viewModel.addSongToPlaylist(playlist.id, song)
                                            }
                                            onDismiss()
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Placeholder Icon
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                         Text(playlist.name.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "${playlist.songs.size} 首歌曲",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            }
                            
                            item {
                                // Create New Playlist Item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable { 
                                            // Ideally show Create Dialog, but for now we might need to handle this
                                            // Simplification: Not supported in this overlay yet, user can go to Playlists tab.
                                           // Or just dismiss to prompt user.
                                           // Keeping simple.
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                     Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(AccentBlue.copy(0.1f)), contentAlignment = Alignment.Center) {
                                         Icon(Icons.Default.Add, null, tint = AccentBlue)
                                     }
                                     Spacer(modifier = Modifier.width(16.dp))
                                     Text("新建歌单 (请前往歌单页)", color = AccentBlue)
                                }
                            }

                            } else if (customListMode) {
                             // Custom List Mode (Favorites / Recents / Removed)
                             itemsIndexed(customSongs) { index, song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { 
                                            // Handle Restore Mode
                                            if (customTitle == "已移除歌曲") {
                                                viewModel.restoreSongs(listOf(song))
                                                // Don't auto-dismiss, user might want to restore multiple
                                            } else {
                                                // Normal Play
                                                viewModel.playSong(song, customSongs, customTitle ?: "Custom List")
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Index
                                    Box(
                                        modifier = Modifier.width(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (customTitle == "已移除歌曲") {
                                            androidx.compose.material3.Icon(
                                                androidx.compose.material.icons.Icons.Default.Refresh,
                                                null,
                                                tint = AccentBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    // 2. Album Cover
                                    AsyncImage(
                                        model = song.albumArtUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // 3. Text Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.Black,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = song.artist,
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    // 4. Action Text for Removed
                                    if (customTitle == "已移除歌曲") {
                                        Text("恢复", fontSize = 14.sp, color = AccentBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
                                    }
                                }
                                HorizontalDivider(
                                    color = Color.Black.copy(alpha = 0.05f), 
                                    modifier = Modifier.padding(start = 92.dp)
                                ) 
                            }
                            
                            if (customSongs.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("暂无歌曲", color = Color.Gray)
                                    }
                                }
                            }

                        } else {
                            // Existing Queue Logic
                            itemsIndexed(currentQueue, key = { _, song -> song.id }) { index, song ->
                                val isCurrent = song.id == currentSong?.id
                                
                                val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.removeFromQueue(index)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )
                                
                                androidx.compose.material3.SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = androidx.compose.animation.animateColorAsState(
                                            if (dismissState.targetValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) Color.Red.copy(alpha = 0.8f) else Color.Transparent,
                                            label = "SwipeColor"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(color.value)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            // Hide icon if not swiping (prevent ghosting through transparent content)
                                            val iconAlpha by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (dismissState.targetValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) 1f else 0f,
                                                label = "IconAlpha"
                                            )
                                            
                                            androidx.compose.material3.Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White.copy(alpha = iconAlpha) // Animate Tint Alpha
                                            )
                                        }
                                    },
                                    enableDismissFromStartToEnd = false
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.01f)) // Capture clicks
                                            .clickable { viewModel.playFromQueue(index) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Indicator / Index
                                        Box(
                                            modifier = Modifier.width(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    null,
                                                    tint = AccentBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.Black,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        
                                        // 2. Album Cover
                                        AsyncImage(
                                            model = song.albumArtUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.LightGray)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))

                                        // 3. Text Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = if (isCurrent) AccentBlue else Color.Black,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = song.artist,
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (index < currentQueue.lastIndex) {
                                    HorizontalDivider(
                                        color = Color.Black.copy(alpha = 0.05f), 
                                        modifier = Modifier.padding(start = 92.dp)
                                    ) 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
