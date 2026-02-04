package com.vagueplayer.music.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vagueplayer.music.ui.components.GlassProgressSlider
import com.vagueplayer.music.viewmodel.AudioViewModel


/**
 * Lyrics Screen - Apple Music Style
 * Reference: Dynamic Liquid Background + Blurred Inactive Lyrics + Bottom Glass Controls
 */
@Composable
fun LyricsScreen(
    viewModel: AudioViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    
    val lyrics by viewModel.lyrics.collectAsState()
    val currentIndex by viewModel.currentLyricIndex.collectAsState()
    val listState = rememberLazyListState()

    // Auto-Scroll Logic with Smooth Animation
    LaunchedEffect(currentIndex) {
        if (lyrics.isNotEmpty() && currentIndex >= 0 && currentIndex < lyrics.size) {
            // Smooth scroll to center the current lyric line
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -300 // Offset to position current line in upper-center
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // 1. Blurred Album Art Background (Same as PlayerScreen)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentSong?.albumArtUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
                .graphicsLayer { alpha = 0.7f },
            contentScale = ContentScale.Crop
        )
        
        // 2. Dark Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 2. Header (Song Info & Menu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formatting: Tiny Album Art + Text
                AsyncImage(
                    model = currentSong?.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentSong?.title ?: "Unknown",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        currentSong?.artist ?: "Unknown",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
                
                // Actions (Close)

                IconButton(onClick = onDismiss) {
                     Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Lyrics List
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp), // Massive padding for focus area
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isCurrent = index == currentIndex
                        val isPast = index < currentIndex
                        val isDragged by listState.interactionSource.collectIsDraggedAsState()
                        
                        // Animation Targets - Overshoot Bounce Effect
                        val targetAlpha = if (isCurrent) 1f else 0.35f
                        val offsetY = remember { Animatable(0f, Float.VectorConverter) }
                        val alpha = remember { Animatable(0.35f) }
                        val scale = remember { Animatable(0.85f) }
                        val blur = remember { Animatable(4f) } // Use Float for Blur radius to be compatible with Animatable easily, or convert
                        // Blur usually needs Dp. Let's stick to Float and .dp in modifier.
                        
                        LaunchedEffect(currentIndex) {
                            val distance = kotlin.math.abs(index - currentIndex) 
                            val delay = distance * 175L // 175ms per line
                            
                            // 1. Position Logic (Counter-Scroll Snap for Future lines)
                            if (index > currentIndex) {
                                offsetY.snapTo(120f) 
                                kotlinx.coroutines.delay(delay)
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            } else {
                                // For Current/Past
                                kotlinx.coroutines.delay(delay)
                                offsetY.animateTo(0f)
                            }
                        }
                        
                        // Combined LaunchedEffect for all properties to ensure sync
                        LaunchedEffect(currentIndex, isCurrent, isPast, isDragged) {
                            val distance = kotlin.math.abs(index - currentIndex)
                            val delay = distance * 175L // 175ms per line
                            
                            // Wait for stagger
                            kotlinx.coroutines.delay(delay)
                            
                            launch {
                                val targetAlphaVal = if (isCurrent) 1f else 0.35f
                                alpha.animateTo(targetAlphaVal, tween(500))
                            }
                            
                            launch {
                                val targetScaleVal = if (isCurrent) 1.08f else 0.85f
                                scale.animateTo(targetScaleVal, spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ))
                            }
                            
                            launch {
                                val targetBlurVal = if (isCurrent || isDragged) 0f else 4f
                                blur.animateTo(targetBlurVal, tween(300))
                            }
                        }

                        // Alignment Logic (Apple Music Style Duet)
                        // If line starts with '(' or '（', treat as backing vocal -> Right Align
                        val isRightAligned = line.text.trim().startsWith("(") || line.text.trim().startsWith("（")
                        val alignment = if (isRightAligned) Alignment.End else Alignment.Start
                        val textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start
                        
                        // Bilingual Lyrics Display: Original on top, Translation below
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .graphicsLayer {
                                    this.alpha = alpha.value
                                    this.scaleX = scale.value
                                    this.scaleY = scale.value
                                    this.translationY = offsetY.value 
                                    this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                        if (isRightAligned) 1f else 0f, 
                                        0.5f
                                    )
                                }
                                .then(
                                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                                        Modifier.blur(blur.value.dp)
                                    } else Modifier
                                )
                                .clickable {
                                    viewModel.seekAndPlay(line.timeMs)
                                },
                            horizontalAlignment = alignment
                        ) {
                            // Calculate Line Progress (for Karaoke)
                            val nextLineTime = if (index < lyrics.size - 1) lyrics[index + 1].timeMs else (line.timeMs + 5000)
                            val lineDuration = (nextLineTime - line.timeMs).coerceAtLeast(1)
                            val lineProgress = ((progress - line.timeMs) / lineDuration.toFloat()).coerceIn(0f, 1f)

                            // Original Text (Primary)
                            if (isCurrent && isPlaying) {
                                // KARAOKE MODE for Current Line
                                KaraokeLine(
                                    text = line.text,
                                    lineProgress = lineProgress,
                                    currentTime = progress.toLong(),
                                    syllables = line.syllables,
                                    isRightAligned = isRightAligned,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // STATIC TEXT for Other Lines
                                Text(
                                    text = line.text,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 38.sp,
                                    textAlign = textAlign
                                )
                            }
                            
                            // Translation (Secondary - smaller, lighter)
                            if (!line.translation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = line.translation,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 28.sp,
                                    textAlign = textAlign
                                )
                            }
                        }
                    }
                    
                    if (lyrics.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No lyrics available", color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
            

        }
    }
}

@Composable
fun GlassControlsCapsule(
    viewModel: AudioViewModel,
    progress: Float,
    duration: Long,
    isPlaying: Boolean
) {
    // A Floating Glass Pill containing controls
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color.White.copy(alpha = 0.2f)) // 20% White Tint (Unification)
            // Could add waterDropGlass here too, but nested might be heavy?
            // Let's keep it simple translucent for contrast against the big glass background.
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Play/Pause
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Progress Slider
            Box(modifier = Modifier.weight(1f)) {
                 // Local State for Dragging
                 var isDragging by remember { mutableStateOf(false) }
                 var dragProgress by remember { mutableFloatStateOf(0f) }
                 val currentProgress = if (isDragging) dragProgress else (if (duration > 0) progress / duration.toFloat() else 0f)

                 GlassProgressSlider(
                     value = currentProgress,
                     onValueChange = { 
                        isDragging = true
                        dragProgress = it 
                     },
                     onValueChangeFinished = { 
                         // Use local dragProgress state
                         viewModel.seekTo((dragProgress * duration).toLong())
                         isDragging = false
                     },
                     modifier = Modifier.fillMaxWidth().height(40.dp) // Increased for Capsule Thumb
                 )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Next
            IconButton(onClick = { viewModel.skipNext() }) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaraokeLine(
    text: String,
    lineProgress: Float, // Fallback for linear interpolation [0..1]
    currentTime: Long = 0L, // Absolute current time
    syllables: List<Pair<Long, String>>? = null, // Syllable data
    isRightAligned: Boolean = false,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(4.dp), 
        horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        if (syllables != null && syllables.isNotEmpty()) {
            // --- SYLLABLE MODE (Progressive Gradient Fill + Dynamic Stiffness) ---
            syllables.forEachIndexed { index, (startTime, syllableText) ->
                val nextTimestamp = if (index < syllables.size - 1) syllables[index + 1].first else (startTime + 500)
                val duration = (nextTimestamp - startTime).coerceAtLeast(1)
                
                // Calculate fill progress for this SPECIFIC syllable
                val syllableProgress = ((currentTime - startTime).toFloat() / duration).coerceIn(0f, 1f)
                
                val isActive = currentTime in startTime until nextTimestamp
                val isPassed = currentTime >= nextTimestamp
                
                // Traffic Light Logic:
                // Passed (Finished) -> Fast Start (high stiffness)
                // Active (Singing):
                // - Leader (Index 0): Fast (400f)
                // - Second (Index 1): Medium (200f) "Ready to start"
                // - Others: Slow (80f) "Reaction delay"
                
                val isLeader = index == 0
                val isSecond = index == 1
                
                val stiffness = when {
                    isPassed -> 400f
                    isLeader -> 400f
                    isSecond -> 200f // "Ready to start" - faster than tail but slower than leader
                    else -> 80f
                }
                
                val damping = when {
                    isPassed || isLeader -> 0.75f
                    isSecond -> 0.82f
                    else -> 0.85f
                }
                
                // Lift/Scale Specs
                val targetScale = if (isActive) 1.04f else 1.0f 
                val targetOffsetY = if (isActive || isPassed) -6f else 0f 
                
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                    label = "sylScale"
                )
                
                val animatedOffsetY by animateFloatAsState(
                    targetValue = targetOffsetY,
                    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                    label = "sylOffset"
                )

                // Progressive Gradient Brush
                val brush = Brush.horizontalGradient(
                    0.0f to Color.White,
                    syllableProgress to Color.White,
                    syllableProgress.coerceAtMost(0.999f) + 0.001f to Color.White.copy(alpha = 0.5f),
                    1.0f to Color.White.copy(alpha = 0.5f)
                )

                Text(
                    text = syllableText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = brush, 
                        shadow = if (isActive || isPassed) androidx.compose.ui.graphics.Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            blurRadius = 8f 
                        ) else null
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationY = animatedOffsetY
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f) // Bottom Left
                            alpha = 1f 
                        }
                )
            }
        
        } else {
            // --- CHAR MODE (Linear Fallback) ---
            // Still fill progressive if we don't have syllables? 
            // The 'lineProgress' is 0..1 for the WHOLE line.
            // Distribute it across characters.
            
            val chars = remember(text) { text.toList() }
            chars.forEachIndexed { index, char ->
                val charStart = index.toFloat() / chars.size
                val charEnd = (index + 1).toFloat() / chars.size
                
                // Map global line progress to local char progress 0..1
                // progress 0.1, char 0..0.1 -> fill 1.0
                // progress 0.05, char 0..0.1 -> fill 0.5
                val localProgress = ((lineProgress - charStart) / (charEnd - charStart)).coerceIn(0f, 1f)
                
                val isActive = localProgress > 0f && localProgress < 1f
                val isPassed = localProgress >= 1f
                
                val targetScale = if (isActive) 1.04f else 1.0f
                val targetOffsetY = if (isActive || isPassed) -6f else 0f
                
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 80f),
                    label = "scale"
                )
                val animatedOffsetY by animateFloatAsState(
                    targetValue = targetOffsetY, 
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 80f),
                    label = "offset"
                )
                
                val brush = Brush.horizontalGradient(
                    0.0f to Color.White,
                    localProgress to Color.White,
                    localProgress + 0.001f to Color.White.copy(alpha = 0.5f),
                    1.0f to Color.White.copy(alpha = 0.5f)
                )

                Text(
                    text = char.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = brush,
                        shadow = if (isActive) androidx.compose.ui.graphics.Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            blurRadius = 12f
                        ) else null
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(end = 1.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationY = animatedOffsetY
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.8f) 
                        }
                )
            }
        }
    }
}
