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
import androidx.compose.material.icons.filled.ArrowBack
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
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
    
    val scrollState = rememberScrollState()

    // 1. Root Box: Replaces Scaffold/Surface for absolute control.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 2. Scrollable Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            
            // [Layer 1] Top Header Placeholder
            // This box reserves space for the image but the image is inside it (unlike user snippet suggestion implying outside, keeping it simple inside is robust)
            // User snippet put Image INSIDE this box. I will follow that.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                val displaySong = coverSong ?: currentPlaylist.songs.firstOrNull()
                val coverUrl = displaySong?.albumArtUri?.toString() // Get URL for cache key

                // A. Cover Image
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            // 🔥 CRITICAL FIX: Disable crossfade to prevent transparency flash
                            .crossfade(false)
                            // 🔥 CRITICAL FIX: Reuse memory cache to eliminate loading gap
                            .placeholderMemoryCacheKey("cover_${currentPlaylist.id}")
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            // Elevate Z-Index to stay above background
                            .zIndex(1f) 
                            .sharedElement(
                                state = rememberSharedContentState(key = "cover_${currentPlaylist.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
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
                
                // B. Gradient Overlay (Layer 2)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 300f
                            )
                        )
                )

                // C. Title (Layer 3)
                Text(
                    text = currentPlaylist.name,
                    style = MaterialTheme.typography.headlineMedium.copy( // Changed to HeadlineMedium per user snippet
                         fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .zIndex(3f)
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
            }

            // [Layer 2] Bottom Content (Song List)
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
                
                // Real Song List Data
                if (currentPlaylist.songs.isEmpty()) {
                    Text("暂无歌曲", color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                     currentPlaylist.songs.forEachIndexed { index, song ->
                         ListItem(
                             headlineContent = { 
                                 Text(
                                     song.title, 
                                     maxLines = 1, 
                                     overflow = TextOverflow.Ellipsis
                                 ) 
                             },
                             supportingContent = { 
                                 Text(
                                     "${song.artist} - ${song.album}",
                                     maxLines = 1,
                                     overflow = TextOverflow.Ellipsis
                                 )
                             },
                             leadingContent = { 
                                  Text(
                                      "${index + 1}", 
                                      color = Color.Gray,
                                      style = MaterialTheme.typography.bodyMedium
                                  ) 
                             },
                             modifier = Modifier.clickable { 
                                 viewModel.playSong(song, currentPlaylist.songs, currentPlaylist.name)
                             }
                         )
                     }
                }
                
                // Bottom Spacing (System Bars + Extra)
                Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp))
            }
        }
        
        // 3. Floating Back Button (Layer Top - Fixed)
        IconButton(
            onClick = onDismissRequest,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .zIndex(100f) // Always on top
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, // Changed to ArrowBack per snippet
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
    
    // Dialogs
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
