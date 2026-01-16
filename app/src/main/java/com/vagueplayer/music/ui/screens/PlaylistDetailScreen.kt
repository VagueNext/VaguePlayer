package com.vagueplayer.music.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistDetailScreen(
    playlist: Playlist,
    coverUrl: String?, // Pass URL directly from parent
    viewModel: AudioViewModel,
    onDismissRequest: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val scrollState = rememberScrollState()

    // 1. Root Box: Full Screen, No Padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 2. Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            
            // Header Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // A. Cover Image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        // 🔥 CRITICAL: Disable crossfade in Target
                        .crossfade(false)
                        // 🔥 CRITICAL: Use SAME URL string as key
                        .placeholderMemoryCacheKey(coverUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .sharedElement(
                            state = rememberSharedContentState(key = "cover_${playlist.id}"), // Keep ID-based key for Geometry
                            animatedVisibilityScope = animatedVisibilityScope,
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = 380f)
                            }
                        )
                )

                // B. Gradient
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

                // C. Title
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .zIndex(3f)
                        .sharedElement(
                            state = rememberSharedContentState(key = "playlist_title_${playlist.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) }
                        )
                )
            }

            // Bottom List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (playlist.songs.isNotEmpty()) {
                            viewModel.playSong(playlist.songs.first(), playlist.songs, playlist.name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("全部播放 (${playlist.songs.size})")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                playlist.songs.forEachIndexed { index, song ->
                     ListItem(
                         headlineContent = { 
                             Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                         },
                         supportingContent = { 
                             Text("${song.artist} - ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis) 
                         },
                         leadingContent = { 
                              Text("${index + 1}", color = Color.Gray) 
                         },
                         modifier = Modifier.clickable { 
                             viewModel.playSong(song, playlist.songs, playlist.name)
                         }
                     )
                }
                
                Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp))
            }
        }

        // 3. Floating Back Button
        IconButton(
            onClick = onDismissRequest,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .zIndex(100f)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
