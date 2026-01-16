package com.vagueplayer.music.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import com.vagueplayer.music.ui.theme.AccentBlue
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import dev.chrisbanes.haze.haze // Add missing import

@Composable
fun GlassProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isGlassEnabled: Boolean = true, // CONTROL FLAG
    hazeState: HazeState? = null, // Injectable source
    onInteractionChange: (Boolean) -> Unit = {}
) {
    // Local interaction state
    var internalInteracting by remember { mutableStateOf(false) }
    
    // Use external if provided, otherwise internal
    val effectiveInteracting = internalInteracting

    // Dynamic Animation Spec based on interaction state
    // 使用统一的弹性形变系统 (Unified ElasticDeformation)
    val animationSpec = if (effectiveInteracting) {
        androidx.compose.animation.core.spring<Float>(dampingRatio = 0.6f, stiffness = 800f)
    } else {
        androidx.compose.animation.core.spring<Float>(dampingRatio = 0.4f, stiffness = 300f)
    }

    // 4. Interaction Animation: SCALE Strategy
    // Using Layout Animation (changing width/height) caused SIGSEGV due to unstable RenderNode texture resizing with Shader.
    // Switching to Modifier.scale() is safer as the underlying RenderNode stays constant size.
    
    // Fixed Base Dimensions (Compact Capsule)
    val baseWidth = 48.dp
    val baseHeight = 24.dp
    
    // 4. Interaction Animation: SCALE Strategy
    // User Request: "Magnify on click, retract on release"
    // Non-linear physics: Snappy Down, Jelly Up (handled by animationSpec)
    val currentScale by animateFloatAsState(targetValue = if (effectiveInteracting) 1.2f else 1.0f, animationSpec = animationSpec, label = "GlassScale")

    // Physics
    // We scale the edge width and distortion based on interaction.
    // Inactive (Small): 
    val currentEdgeWidth by animateFloatAsState(
        targetValue = 20.0f, // Narrower constant width (requested)
        label = "Edge Width"
    )
    // BOOST: Use much higher distortion for the Lens Effect (was 60/30)
    // Adjusted for new Shader Logic (1.5x multiplier + Spherize): Less is More now.
    val currentDistortion by animateFloatAsState(
        targetValue = if (effectiveInteracting) 65.0f else 50.0f, // Reduced peak to 65.0f
        label = "Distortion Strength"
    )

    // Local Haze State REMOVED for external injection
    // val localHazeState = remember { HazeState() }

    // 5. Track Animation: Scale Height
    // [FIX] Decoupled from Thumb Interaction (Always 1.0f)
    val currentTrackScale by animateFloatAsState(
        targetValue = 1.0f, 
        animationSpec = animationSpec,
        label = "TrackScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp) // Touch Area Height
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        internalInteracting = true
                        onInteractionChange(true)
                        
                        val width = size.width.toFloat()
                        val newValue = (down.position.x / width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                        
                        var active = true
                        while (active) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val isUp = changes.all { !it.pressed }
                            
                            if (isUp) {
                                active = false
                                internalInteracting = false
                                onInteractionChange(false)
                                onValueChangeFinished()
                            } else {
                                val change = changes.firstOrNull { it.pressed } ?: continue
                                change.consume()
                                val currentX = change.position.x
                                val dragValue = (currentX / width).coerceIn(0f, 1f)
                                onValueChange(dragValue)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val baseTrackHeight = 4.dp
        val animatedTrackHeight = baseTrackHeight * currentTrackScale
        
        // Thumb (Glass)
        // FIX: Instead of scaling the layer (which scales the background texture and causes "Magnification/Offset"),
        // we must animate the physical Layout Size of the box. 
        // This ensures Haze always samples the background at 1:1 scale for the new size.
        val animatedThumbWidth = baseWidth * currentScale
        val animatedThumbHeight = baseHeight * currentScale
        val thumbWidthPx = with(LocalDensity.current) { animatedThumbWidth.toPx() }
        
        // precise centering with animated width
        val centerPos = totalWidth * value
        val offsetX = centerPos - (thumbWidthPx / 2)
        
        // Active Track Alignment
        // [FIX] Stop Track BEFORE the Glass Thumb to avoid "Binding" / internal refraction.
        // The active track should end exactly where the thumb begins.
        val activeTrackWidthPx = offsetX.coerceAtLeast(0f)
        val activeTrackWidth = with(LocalDensity.current) { activeTrackWidthPx.toDp() }

        GlassTrack(
            modifier = Modifier.fillMaxWidth(),
            currentTrackScale = currentTrackScale,
            activeTrackWidth = activeTrackWidth,
            animatedTrackHeight = animatedTrackHeight
        )
        
        GlassThumb(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .align(Alignment.CenterStart),
            width = animatedThumbWidth,
            height = animatedThumbHeight,
            edgeWidth = currentEdgeWidth, distortionStrength = currentDistortion,
            trackScale = currentTrackScale, 
            hazeState = hazeState,
            isGlassEnabled = isGlassEnabled
        )
    }
}

@Composable
fun GlassTrack(
    modifier: Modifier = Modifier,
    currentTrackScale: Float,
    activeTrackWidth: Dp,
    animatedTrackHeight: Dp
) {
    Box(modifier = modifier) {
        // Inactive
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedTrackHeight)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(100))
                .background(Color.White.copy(alpha = 0.3f))
        )
        // Active
        Box(
            modifier = Modifier
                .width(activeTrackWidth)
                .height(animatedTrackHeight)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50))
                .background(Color.White)
        )
    }
}

@Composable
fun GlassThumb(
    modifier: Modifier,
    width: Dp,
    height: Dp,
    edgeWidth: Float,
    distortionStrength: Float,
    trackScale: Float, // New parameter
    hazeState: HazeState?,
    isGlassEnabled: Boolean
) {
    // Force Real-time Updates:
    // We use an infinite transition to drive the 'time' parameter.
    // This forces the RenderEffect to update/invalidate every frame, ensuring dynamic background content (like moving album art)
    // is accurately refracted without freezing/caching artifacts.
    val infiniteTransition = rememberInfiniteTransition(label = "RealtimeGlass")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlassTicker"
    )

    // We use Box to hold the specific "Glass" visual
    // FIX: Removed .scale(thumbScale). The width/height are already scaled.
    // val trackHeightPx = with(LocalDensity.current) { (4.dp * trackScale).toPx() } // Unused now

    Box(
        modifier = modifier
            .size(width = width, height = height)
            // Keep the shader for background blur/distortion if needed, 
            // but we might override visual content.
            // Let's keep waterDropGlass for the physical "Material" properties (blur, edge distortion of background)
            .waterDropGlass(
                hazeState = if (isGlassEnabled) hazeState else null, // [RESTORE] Need Haze for Distortion source
                // Match Player Controls White (Milky Glass)
                blurRadius = 0.dp, // [KEEP] Blur = 0 as requested
                tint = if (isGlassEnabled) Color.White.copy(alpha = 0.05f) else Color.White, 
                cornerRadius = height / 2,
                edgeWidth = if (isGlassEnabled) edgeWidth else 0f, 
                distortionStrength = if (isGlassEnabled) distortionStrength else 0f, // [RESTORE] Distortion enabled
                aberrationStrength = 0f, 
                enableShader = isGlassEnabled,
                time = time 
            )
    ) {
        // Canvas Removed:
        // We now rely 100% on the Shader for all visual effects (Refraction + Highlights + Rim).
        // Mixing Manual Canvas + Shader caused "Double Layer" artifacts.
    }
}
