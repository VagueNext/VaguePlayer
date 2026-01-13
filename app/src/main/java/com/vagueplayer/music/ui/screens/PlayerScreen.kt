package com.vagueplayer.music.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.List
import com.vagueplayer.music.ui.components.RoundedRepeatIcon
import com.vagueplayer.music.ui.components.RoundedShuffleIcon // [NEW] Added Import
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.ui.components.GlassProgressSlider
import com.vagueplayer.music.ui.components.GlassDialog 
import com.vagueplayer.music.ui.components.waterDropGlass
import com.vagueplayer.music.ui.components.bouncyClickable
import com.vagueplayer.music.ui.components.RoundedRepeatIcon
import com.vagueplayer.music.ui.components.RoundedShuffleIcon
import com.vagueplayer.music.viewmodel.AudioViewModel
import androidx.media3.common.Player
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned

// Utility
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    viewModel: AudioViewModel,
    onDismiss: () -> Unit,
    onTogglePlaylist: () -> Unit,
    hazeState: HazeState? = null
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val progress by viewModel.progress.collectAsState()

    // Lyrics State (Internal)
    var isLyricsVisible by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }
    var showLoopCountDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) } // [NEW] Sleep Timer State
    
    // Lifted state for access in Overlay
    val repeatMode by viewModel.repeatMode.collectAsState()


    // Local HazeState (or Shared)
    val finalHazeState = hazeState ?: remember { HazeState() }
    
    // --- PORTAL STATE HOISTING START ---
    // 1. Progress State
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    
    // 2. Slider Interaction State
    var isSliderInteracting by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var rootPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) } // [NEW] Capture Root Pos
     
    // 3. Replicate Animations for Overlay Thumb
    val animationSpec = if (isSliderInteracting) {
        androidx.compose.animation.core.spring<Float>(dampingRatio = 0.6f, stiffness = 800f) 
    } else {
        androidx.compose.animation.core.spring<Float>(dampingRatio = 0.4f, stiffness = 300f)
    }
    val currentScale by animateFloatAsState(targetValue = if (isSliderInteracting) 1.2f else 1.0f, animationSpec = animationSpec, label = "GlassScale")
    
    // [FIX] Sync with GlassProgressSlider: Narrower Edge (20f) and Scale it (x1.2) to prevent "Ring Pop" artifact
    val currentEdgeWidth by animateFloatAsState(
        targetValue = if (isSliderInteracting) 24.0f else 20.0f, 
        label = "Edge Width"
    )
    
    // [FIX] Animate Distortion: 50.0 (Idle) -> 65.0 (Active)
    val currentDistortion by animateFloatAsState(
        targetValue = if (isSliderInteracting) 65.0f else 50.0f, 
        label = "Distortion"
    )

    val currentTrackScale by animateFloatAsState(targetValue = if (isSliderInteracting) 1.5f else 1.0f, animationSpec = animationSpec, label = "TrackScale")
    // val isLiquidEnabled REMOVED (Enforced Liquid Glass)
    // --- PORTAL STATE HOISTING END ---

    val context = LocalContext.current

    // --- LIQUID COLOR LOGIC REMOVED (White Background Only) ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned {
                val pos = it.positionInRoot()
                if (pos.getDistance() > 0) rootPosition = pos
            } // [NEW] Capture Global Root Pos Only if Valid
            .pointerInput(Unit) {
                var accumulation = androidx.compose.ui.geometry.Offset.Zero
                var triggered = false

                detectDragGestures(
                    onDragStart = { 
                        accumulation = androidx.compose.ui.geometry.Offset.Zero 
                        triggered = false
                    },
                    onDragEnd = { triggered = false },
                    onDragCancel = { triggered = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulation += dragAmount
                        
                        if (!triggered) {
                            val absX = kotlin.math.abs(accumulation.x)
                            val absY = kotlin.math.abs(accumulation.y)
                            
                            // 1. Vertical Swipe (Dismiss / Lyrics) - Priority: Must be clearly vertical
                            if (absY > absX * 1.5f) { // Steep angle required
                                if (accumulation.y > 80) { // Swipe Down -> Dismiss (Easier)
                                    onDismiss()
                                    triggered = true
                                } else if (accumulation.y < -50) { // Swipe Up -> Lyrics (Very Easy)
                                    isLyricsVisible = true
                                    triggered = true
                                }
                            }
                            // 2. Horizontal Swipe (Prev / Next)
                            else if (absX > absY * 1.5f) { // Shallow angle required
                                if (accumulation.x > 100) { // Swipe Right -> Previous
                                    viewModel.skipPrevious()
                                    triggered = true
                                } else if (accumulation.x < -100) { // Swipe Left -> Next
                                    viewModel.skipNext()
                                    triggered = true
                                }
                            }
                        }
                    }
                )
            }
            // .haze(state = finalHazeState) // [REMOVED] Root must NOT be Source if it contains Sink
    ) {
        // SOURCE CONTENT WRAPPER: Captures Background + Tracks
        // This Box contains the elements to be blurred/refracted.
        // It does NOT contain the Sinks (GlassThumb, Dialogs), avoiding recursion.
        // [NEW] Haze Source Box: Wraps Background + Track
        // This Box contains the elements to be blurred/refracted.
        // It does NOT contain the Sinks (GlassThumb, Dialogs), avoiding recursion.
        Box(
             modifier = Modifier
                 .fillMaxSize()
                 .haze(state = finalHazeState) 
        ) {
            Box(
                 modifier = Modifier.fillMaxSize()
            ) {
             // A. Background (Pure White)\n             Box(modifier = Modifier.fillMaxSize().background(Color.White))
            
             // B. Album Art Overlay (Blurred)
             AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentSong?.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp) 
            )
            // Lighter Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // 2. Main Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A. Top Grab Handle [REMOVED]
            Spacer(modifier = Modifier.weight(1f))
            
            // B. Large Album Art
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(20.dp, RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = currentSong?.albumArtUri,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // C. Metadata & Icons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Not Playing",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong?.artist ?: "Vague Player",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                
                // Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val favoriteIds by viewModel.favoriteIds.collectAsState()
                    val isFavorite = currentSong?.let { favoriteIds.contains(it.id) } == true
                    
                    IconButton(onClick = { currentSong?.let { viewModel.toggleFavorite(it.id) } }) { 
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, 
                            contentDescription = "Favorite", 
                            tint = if (isFavorite) Color.White else Color.Gray, // Active: White, Inactive: Gray
                            modifier = Modifier.size(28.dp)
                        ) 
                    }
                    // [REMOVED] MoreHoriz Icon
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // D. Progress Slider
            // State Moved to Top Level for Portal Access
            val currentProgress = if (isDragging) dragProgress else (if (duration > 0) progress / duration.toFloat() else 0f)



            // 2. The Slider (Source Element - Render Tracks only)
                GlassProgressSlider(
                    value = if (isDragging) dragProgress else progress / duration.toFloat().coerceAtLeast(1f),
                    onValueChange = { newPercent ->
                        isDragging = true
                        dragProgress = newPercent
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        viewModel.seekTo((dragProgress * duration).toLong())
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                    isGlassEnabled = true, // [ENFORCED Liquid Glass]
                    hazeState = null, // [FIX] Revert to null. Slider is already in Source Wrapper!
                    renderThumb = false, // We render the sink physically outside
                    onInteractionChange = { isSliderInteracting = it },
                    onLayoutCoordinates = { 
                        val pos = it.positionInRoot()
                        if (pos.getDistance() > 0) sliderPosition = pos
                    }
                )
            
            // 3. Portal Sink: Glass Thumb Overlay
            // Rendered here, outside the Haze Source, as a sibling.
            if (duration > 0) { // [ENFORCED Liquid Glass] 
                 // Calculate Thumb Offset based on progress
                 // We need the width of the slider to calculate offset. 
                 // Assuming standard padding (24dp horizontal). The slider fills width.
                 // Wait, we need the exact slider width. 
                 // Let's rely on BoxWithConstraints in GlassProgressSlider? No, we are outside.
                 // We can use a BoxOverlay over the whole Column?
                 // Or just place it in a sibling Box that matches the Column alignment?
            }
            
            // Time Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                     text = formatTime(if(isDragging) (dragProgress * duration).toLong() else progress.toLong()),
                     fontSize = 12.sp,
                     color = Color.White.copy(alpha = 0.7f),
                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                     fontWeight = FontWeight.Medium
                )
                Text(
                     text = "-" + formatTime(duration - (if(isDragging) (dragProgress * duration).toLong() else progress.toLong())),
                     fontSize = 12.sp,
                     color = Color.White.copy(alpha = 0.7f),
                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                     fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev
                Box(
                    modifier = Modifier
                        .size(60.dp) // Larger target
                        .bouncyClickable(targetScale = 1.15f, onClick = { viewModel.skipPrevious() }),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         imageVector = Icons.Rounded.FastRewind, 
                         contentDescription = "Previous", 
                         tint = Color.White, 
                         modifier = Modifier.size(42.dp)
                     )
                }
                
                // Play/Pause - NO CIRCLE BACKGROUND
                Box(
                    modifier = Modifier
                        .size(80.dp) // Larger container
                        .clip(CircleShape) // Clip for ripple
                        .bouncyClickable(
                            targetScale = 1.15f,
                            onClick = { viewModel.togglePlayPause() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                         contentDescription = null,
                         tint = Color.White,
                         modifier = Modifier.size(56.dp)
                     )
                }
                
                // Next
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .bouncyClickable(targetScale = 1.15f, onClick = { viewModel.skipNext() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward, 
                        contentDescription = "Next", 
                        tint = Color.White, 
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // FIX: Avoid system gesture bar overlap
                    .padding(bottom = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LEFT: Repeat Button (Weight 1f)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                     val remainingLoopCount by viewModel.remainingLoopCount.collectAsState()
                     val targetLoopCount by viewModel.targetLoopCount.collectAsState()
                     
                     Box(contentAlignment = Alignment.BottomStart) {
                          Box(
                             modifier = Modifier
                                 .size(50.dp)
                                 .bouncyClickable(
                                     onClick = { viewModel.cyclePlayMode() },
                                     onLongClick = { showRepeatMenu = true }
                                 )
                                 .padding(8.dp),
                             contentAlignment = Alignment.Center
                          ) {
                              val isLoopActive by remember(targetLoopCount) { derivedStateOf { targetLoopCount > 0 } }
                              val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()                             
                              val icon = when {
                                 isLoopActive -> Icons.Default.Repeat 
                                 repeatMode == Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                 else -> Icons.Default.Repeat
                             }
                             
                             val tint = when {
                                 isLoopActive -> Color.White // [USER REQUEST] Standard White
                                 isShuffleEnabled -> Color.White // [USER REQUEST] Standard White
                                 repeatMode == Player.REPEAT_MODE_ONE -> Color.White
                                 repeatMode == Player.REPEAT_MODE_ALL -> Color.White
                                 else -> Color.White.copy(alpha = 0.4f) // [ADJUST] Dimmer for inactive
                             }
                             
                             if (isLoopActive) {
                                  // Custom Loop Icon -> Reusing RoundedRepeatIcon
                                  RoundedRepeatIcon(
                                      count = if (remainingLoopCount > 0) remainingLoopCount.toString() else "∞",
                                      color = tint,
                                      modifier = Modifier.size(24.dp)
                                  )
                             } else {
                                  if (repeatMode == Player.REPEAT_MODE_OFF && !isShuffleEnabled) {
                                      Icon(
                                          imageVector = icon,
                                          contentDescription = "Repeat",
                                          tint = tint,
                                          modifier = Modifier.size(24.dp)
                                      )
                                  } else if (isShuffleEnabled) {
                                      RoundedShuffleIcon(
                                          color = tint,
                                          modifier = Modifier.size(24.dp)
                                      )
                                  } else {
                                       when (repeatMode) {
                                         Player.REPEAT_MODE_ONE -> RoundedRepeatIcon(count = "1", color = tint, modifier = Modifier.size(24.dp))
                                         Player.REPEAT_MODE_ALL -> RoundedRepeatIcon(count = null, color = tint, modifier = Modifier.size(24.dp))
                                         else -> RoundedRepeatIcon(count = null, color = tint.copy(alpha = 0.3f), modifier = Modifier.size(24.dp)) 
                                      }
                                  }
                             }
                          }
                     }
                }
                
                // 2. CENTER: Sleep Timer Button (Weight 1f)
                val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
                val isTimerActive = sleepTimerRemaining != null
                
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .bouncyClickable(onClick = { showSleepTimerDialog = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Timer, 
                                contentDescription = "Sleep Timer",
                                tint = if (isTimerActive) com.vagueplayer.music.ui.theme.AccentBlue else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                            if (isTimerActive) {
                                val mins = kotlin.math.ceil((sleepTimerRemaining ?: 0) / 60000.0).toInt()
                                Text(
                                    text = "${mins}m",
                                    fontSize = 10.sp,
                                    color = com.vagueplayer.music.ui.theme.AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. RIGHT: List Button (Weight 1f)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp) // Maintain touch target size
                            .bouncyClickable(onClick = { onTogglePlaylist() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                    }
                }
            }
            
            }
        } // END SOURCE WRAPPER
        
        // 3. Repeat Menu Overlay
        // SIBLING TO SOURCE (No Recursion)
        if (showRepeatMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showRepeatMenu = false },
                contentAlignment = Alignment.BottomStart
            ) {
                com.vagueplayer.music.ui.components.RepeatModeMenu(
                    currentMode = repeatMode,
                    onModeSelected = { mode -> 
                        if (mode == 3) {
                             viewModel.setShuffleMode(true)
                        } else {
                             viewModel.toggleRepeatMode(mode)
                             if (mode != Player.REPEAT_MODE_OFF) viewModel.setShuffleMode(false) 
                        }
                        showRepeatMenu = false 
                    },
                    onSetCount = { 
                        showRepeatMenu = false
                        showLoopCountDialog = true
                    },
                    onDismiss = { showRepeatMenu = false },
                    hazeState = finalHazeState, // [RESTORED]
                    modifier = Modifier.padding(start = 24.dp, bottom = 90.dp)
                )
            }
        }

        // 6. PORTAL SINK LAYER: Glass Thumb Overlay
        // This is now a SIBLING of the Source, ensuring proper Haze capture without recursion.
        if (sliderPosition != androidx.compose.ui.geometry.Offset.Zero && rootPosition != androidx.compose.ui.geometry.Offset.Zero && !isLyricsVisible) { // [ENFORCED Liquid Glass]
             // 1. Calculate Relative Offset
             val relativeX = sliderPosition.x - rootPosition.x
             val relativeY = sliderPosition.y - rootPosition.y
             
             // 2. Thumb Dimensions and Centering
             val baseWidth = 34.dp
             val baseHeight = 20.dp
             val animatedThumbWidth = baseWidth * currentScale
             val animatedThumbHeight = baseHeight * currentScale
             
             // 3. Center Vertically relative to Slider Height (30.dp)
             val sliderHeight = 30.dp
             val verticalCenterOffset = (sliderHeight - animatedThumbHeight) / 2
             
             // 4. Center Horizontally relative to Progress
             val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
             val sliderWidth = screenWidth - 48.dp
             val totalWidthPx = with(LocalDensity.current) { sliderWidth.toPx() }
             val thumbWidthPx = with(LocalDensity.current) { animatedThumbWidth.toPx() }
             
             val progressRatio = if (duration > 0) progress / duration.toFloat() else 0f
             val centerPos = totalWidthPx * if (isDragging) dragProgress else progressRatio
             val thumbOffsetX = centerPos - (thumbWidthPx / 2)
             
             val finalX = relativeX + thumbOffsetX
             val finalY = relativeY + with(LocalDensity.current) { verticalCenterOffset.toPx() }
             
             // Render the Floating Thumb
             com.vagueplayer.music.ui.components.GlassThumb(
                 modifier = Modifier
                     .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
                     .zIndex(100f), 
                 width = animatedThumbWidth,
                 height = animatedThumbHeight,
                 edgeWidth = currentEdgeWidth,
                 distortionStrength = currentDistortion,
                 trackScale = currentTrackScale, 
                 hazeState = finalHazeState, // Shared State
                 isGlassEnabled = true
             )
        }

        // 4. Loop Count Dialog (High Z-Index)
        if (showLoopCountDialog) {
            Box(modifier = Modifier.zIndex(200f).fillMaxSize()) {
                var text by remember { mutableStateOf("1") }
                
                GlassDialog(
                    hazeState = finalHazeState, // [RESTORED]
                    onDismissRequest = { showLoopCountDialog = false },
                    icon = Icons.Default.RepeatOne,
                    title = "循环次数",
                    description = "设置单曲循环 N 次后自动切歌",
                    confirmText = "确认",
                    cancelText = "取消",
                    onConfirm = {
                        val count = text.toIntOrNull() ?: 1
                        viewModel.setLoopCount(count)
                        showLoopCountDialog = false
                    },
                    content = {
                        // Custom Input Field
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = { if (it.all { char -> char.isDigit() }) text = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 32.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                            modifier = Modifier
                                .width(120.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(vertical = 12.dp)
                        )
                    }
                )
            }
        }
        
        // 7. Sleep Timer Dialog (High Z-Index)
        if (showSleepTimerDialog) {
             Box(modifier = Modifier.zIndex(200f).fillMaxSize()) {
                 val remaining by viewModel.sleepTimerRemaining.collectAsState()
                 val currentMin = if (remaining != null) kotlin.math.ceil(remaining!! / 60000.0).toInt() else null
                 
                 com.vagueplayer.music.ui.components.SleepTimerDialog(
                     hazeState = finalHazeState,
                     currentTimerMin = currentMin,
                     onSetTimer = { min ->
                         viewModel.startSleepTimer(min ?: 0)
                     },
                     onDismiss = { showSleepTimerDialog = false }
                 )
             }
        }

        // 8. Lyrics Screen Overlay
        AnimatedVisibility(
            visible = isLyricsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.zIndex(300f) // Topmost Layer
        ) {
            com.vagueplayer.music.ui.screens.LyricsScreen(
                viewModel = viewModel,
                isVisible = true,
                onDismiss = { isLyricsVisible = false },
                hazeState = finalHazeState
            )
        }
    } // End Root Box
}
