package com.vagueplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vagueplayer.music.viewmodel.AudioViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    viewModel: AudioViewModel,
    modifier: Modifier = Modifier,
    collapseProgress: Float = 0f, 
    onExpand: () -> Unit,
    onPlaylistClick: () -> Unit,
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    
    // Swipe State
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeableState = rememberDraggableState { delta ->
        // Dampened movement (0.6 coefficient for better responsiveness)
        scope.launch {
            offsetX.snapTo(offsetX.value + delta * 0.6f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        // Re-implement swipe logic
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxSize() // Already fills max size
                .padding(horizontal = 4.dp)
                .clickable(onClick = onExpand) 
                .draggable(
                    state = swipeableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { 
                        if (offsetX.value.absoluteValue > 60f) {
                            if (offsetX.value > 0) viewModel.skipPrevious() else viewModel.skipNext()
                        }
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 8.dp), // Shift content up to avoid glass distortion at bottom edge
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art / Icon
                // Cover Art Morphing
                val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            state = rememberSharedContentState(key = "album_art"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            renderInOverlayDuringTransition = false
                        )
                    }
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .then(sharedModifier) 
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                ) {
                    AsyncImage(
                        model = currentSong?.albumArtUri,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentSong?.title ?: "Not Playing",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.8f), 
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        modifier = Modifier
                    )
                    Text(
                        text = currentSong?.artist ?: "Vague Player",
                        fontSize = 10.sp,
                        color = Color.Black.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 12.sp
                    )
                }

                // Controls
                // Controls Transparent Style based on user feedback
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                // Playlist Button [ANIMATION] Fade out during collapse
                val playlistAlpha = (1f - collapseProgress * 5).coerceIn(0f, 1f)
                if (playlistAlpha > 0f) {
                    var menuButtonAnchor by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    IconButton(
                        onClick = { 
                            if (isSelectionMode) {
                                currentSong?.let { onSongMenuRequest(it, menuButtonAnchor, androidx.compose.ui.unit.DpSize(32.dp, 32.dp)) }
                            } else {
                                onPlaylistClick() 
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .onGloballyPositioned {
                                val bounds = it.boundsInRoot()
                                menuButtonAnchor = androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom)
                            }
                    ) {
                        Icon(
                            // Show MoreVert (Three Dots) in Selection Mode as requested
                            imageVector = if (isSelectionMode) Icons.Default.MoreVert else Icons.AutoMirrored.Filled.List,
                            contentDescription = "Playlist",
                            tint = Color.Black.copy(alpha = 0.8f * playlistAlpha),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
