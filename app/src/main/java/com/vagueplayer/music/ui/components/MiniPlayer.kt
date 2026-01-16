package com.vagueplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement // [FIX] Missing import
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vagueplayer.music.viewmodel.AudioViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
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
    hazeState: HazeState? = null,
    collapseProgress: Float = 0f, // [NEW] Controls animation
    onExpand: () -> Unit,
    onPlaylistClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null, // [NEW] Shared Element Scope
    animatedVisibilityScope: AnimatedVisibilityScope? = null // [NEW] Visibility Scope
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

    // Floating Capsule
    // Using Surface for better shadow support
// Simplified MiniPlayer for Split-Layer Architecture
// Background is handled by parent (GooeyNavBarContainer Layer 1)
// Function: Haze/Blur effect usually goes on TOP of background or ON the background.
// Issue: If we remove Surface, we lose Haze?
// Wait: GooeyNavBarContainer Layer 1 is WHITE. Liquid.
// The reference image shows "Glassy" look. Glass usually means Background Blur (Haze) + Light/Reflection (Shader).
// In Split-Layer: Layer 1 is the WHITE SHAPE (Liquid).
// If we want Glass, we typically apply Haze to the SHAPE.
// BUT our Shader is applied to the Shape.
// The content (Text) is on top.
// If we want the *entire capsule* to look like glass, the "White Shape" in Layer 1 is what gives it form.
// So removing the Surface here is correct, because Layer 1 draws the capsule.
// However, touch events (onClick) need to be preserved.

    Box(
        modifier = modifier
            .fillMaxWidth(), // [FIX] Removed clickable from parent (blocked by child draggable)
        contentAlignment = Alignment.CenterStart
    ) {
        // Re-implement swipe logic
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxSize()
                .padding(horizontal = 8.dp) // [RESIZE] Reduced padding for more space
                .clickable(onClick = onExpand) // [FIX] Clickable MUST be on the same node as draggable (or above it in modifier chain)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art / Icon [RESIZE]
                // [SHARED ELEMENT] Cover Art Morphing
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
                        .padding(start = 3.dp) // [USER REQUEST] Shift right 2dp (Total 3dp)
                        .then(sharedModifier) // Apply shared element here
                        .size(28.dp) // [RESIZE] User requested 28dp
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

                // Text [RESIZE]
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center // [FIX] Center vertically
                ) {
                    Text(
                        text = currentSong?.title ?: "Not Playing",
                        fontSize = 12.sp, // [RESIZE] Slightly smaller to fit
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.8f), 
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, // [FIX] Ellipsis
                        lineHeight = 14.sp, // [FIX] Tight line height
                        modifier = Modifier
                    )
                    Text(
                        text = currentSong?.artist ?: "Vague Player",
                        fontSize = 10.sp, // [RESIZE] Slightly smaller to fit
                        color = Color.Black.copy(alpha = 0.5f), 
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, // [FIX] Ellipsis
                        lineHeight = 12.sp // [FIX] Tight line height
                    )
                }

                // Controls [RESIZE]
                // Controls [REVISED] Transparent Style based on user feedback
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black, // Black transparent style
                        modifier = Modifier.size(26.dp) 
                    )
                }
                
                // Playlist Button [ANIMATION] Fade out during collapse
                val playlistAlpha = (1f - collapseProgress * 5).coerceIn(0f, 1f)
                if (playlistAlpha > 0f) {
                    IconButton(
                        onClick = { onPlaylistClick() },
                        modifier = Modifier.size(32.dp) // [RESIZE] Compact button
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Playlist",
                            tint = Color.Black.copy(alpha = 0.8f * playlistAlpha),
                            modifier = Modifier.size(20.dp) // [RESIZE]
                        )
                    }
                }
            }
        }
    }
}
