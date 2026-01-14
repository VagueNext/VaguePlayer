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
    onInteractionChange: (Boolean) -> Unit = {},
    renderThumb: Boolean = true,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    // Local interaction state removed in favor of hoisted state or internal state
    // We keep internal state for basic usage if onInteractionChange not used?
    // Actually, to support both modes:
    var internalInteracting by remember { mutableStateOf(false) }
    
    // We use a side effect to report internal changes if needed, but for now let's assume parent drives it OR we drive it.
    // Let's use internal state as driver, and report up.
    val isInteracting = internalInteracting

    // Dynamic Animation Spec based on interaction state
    // 使用统一的弹性形变系统 (Unified ElasticDeformation)
    val animationSpec = if (isInteracting) {
        com.vagueplayer.music.ui.animation.AnimationSpecs.ElasticSnappy  // 按下: 快速响应
    } else {
        com.vagueplayer.music.ui.animation.AnimationSpecs.ElasticJelly   // 释放: 液态回弹
    }

    // 4. Interaction Animation: SCALE Strategy
    // Using Layout Animation (changing width/height) caused SIGSEGV due to unstable RenderNode texture resizing with Shader.
    // Switching to Modifier.scale() is safer as the underlying RenderNode stays constant size.
    
    // Fixed Base Dimensions (Compact Capsule)
    val baseWidth = 34.dp
    val baseHeight = 20.dp
    
    // 4. Interaction Animation: SCALE Strategy
    // User Request: "Magnify on click, retract on release"
    // Non-linear physics: Snappy Down, Jelly Up (handled by animationSpec)
    val currentScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.2f else 1.0f,
        animationSpec = animationSpec,
        label = "GlassScale"
    )

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
        targetValue = if (isInteracting) 65.0f else 50.0f, // Reduced peak to 65.0f
        label = "Distortion Strength"
    )

    // Local Haze State REMOVED for external injection
    // val localHazeState = remember { HazeState() }

    // 5. Track Animation: Scale Height
    val currentTrackScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.5f else 1.0f, // 4dp -> 6dp
        animationSpec = animationSpec,
        label = "TrackScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp) // Touch Area Height
            .then(if (onLayoutCoordinates != null) Modifier.onGloballyPositioned(onLayoutCoordinates) else Modifier)
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
        // FIX: The user perceives the "Round Cap" inside the glass as "Perspective Magnification" (Bulge).
        // To fix this, we Square Off the end of the track so it looks like a clean cut/line, 
        // purely flat and mechanical (No "Lens" effect).
        val activeTrackWidthPx = centerPos 
        val activeTrackWidth = with(LocalDensity.current) { activeTrackWidthPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // [NEW] If thumb is rendered EXTERNALLY, we assume THIS track is the Source
                // If thumb is rendered INTERNALLY, we also need it as Source
                // To support both, we keep it here.
                // .then(if (hazeState != null) Modifier.haze(hazeState) else Modifier) // [REMOVED] Redundant/Dangerous Source
        ) {
            // Inactive
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTrackHeight)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.3f)) // White Track
            )
            // Active
            Box(
                modifier = Modifier
                    .width(activeTrackWidth)
                    .height(animatedTrackHeight)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50))
                    .background(Color.White) // White Active Track
            )
        }
        
        if (renderThumb) {
            GlassThumb(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .align(Alignment.CenterStart),
                width = animatedThumbWidth,
                height = animatedThumbHeight,
                edgeWidth = currentEdgeWidth,
                distortionStrength = currentDistortion,
                trackScale = currentTrackScale, // Pass the track scale
                hazeState = hazeState,
                isGlassEnabled = isGlassEnabled
            )
        }
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
                hazeState = if (isGlassEnabled) hazeState else null, 
                // Match Player Controls White (Milky Glass)
                blurRadius = 0.dp,
                tint = if (isGlassEnabled) Color.White.copy(alpha = 0.2f) else Color.White, // [FIX] Increased visibility (Frosted Glass) 
                cornerRadius = height / 2,
                // Use Passed Parameter (3.dp or calculated)
                edgeWidth = if (isGlassEnabled) edgeWidth else 0f, 
                // Use Passed Parameter (60f or calculated)
                distortionStrength = if (isGlassEnabled) distortionStrength else 0f,
                aberrationStrength = 0f, // DISABLE Rainbow/Green effect completely
                enableShader = isGlassEnabled,
                time = time 
            )
    ) {
        // Canvas Removed:
        // We now rely 100% on the Shader for all visual effects (Refraction + Highlights + Rim).
        // Mixing Manual Canvas + Shader caused "Double Layer" artifacts.
    }
}
