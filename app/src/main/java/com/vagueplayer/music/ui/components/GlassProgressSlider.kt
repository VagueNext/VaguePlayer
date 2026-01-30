
package com.vagueplayer.music.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun GlassProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isGlassEnabled: Boolean = false,
    visible: Boolean = true,
    onLayoutCoordinates: (LayoutCoordinates) -> Unit = {},
    onInteractionChange: (Boolean) -> Unit = {}
) {
    var isInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(isInteracting) {
        onInteractionChange(isInteracting)
    }

    val sliderModifier = modifier
        .height(30.dp)
        .fillMaxWidth()
        .onGloballyPositioned { onLayoutCoordinates(it) }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isInteracting = true

                    val width = size.width.toFloat()
                    val newValue = (down.position.x / width).coerceIn(0f, 1f)
                    onValueChange(newValue)

                    var active = true
                    while (active) {
                        val event = awaitPointerEvent()
                        val isUp = event.changes.all { !it.pressed }

                        if (isUp) {
                            active = false
                            isInteracting = false
                            onValueChangeFinished()
                        } else {
                            val change = event.changes.firstOrNull { it.pressed } ?: continue
                            change.consume()
                            val dragValue = (change.position.x / width).coerceIn(0f, 1f)
                            onValueChange(dragValue)
                        }
                    }
                }
            }
        }

    BoxWithConstraints(
        modifier = sliderModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val baseTrackHeight = 4.dp

        // Fixed Base Dimensions (Horizontal Pill based on Reference)
        val baseWidth = 26.dp
        val baseHeight = 16.dp

        val animationSpec = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.6f, stiffness = 800f)
        val effectiveInteracting = isInteracting // Simplify state

        // 4. Interaction Animation: SCALE Strategy
        // User Request: "Magnify on click, retract on release"
        // Non-linear physics: Snappy Down, Jelly Up (handled by animationSpec)
        val currentScale by animateFloatAsState(targetValue = if (effectiveInteracting) 1.25f else 1.0f, animationSpec = animationSpec, label = "GlassScale") // Slightly less scale since base is larger

        val animatedThumbWidth = baseWidth * currentScale
        val animatedThumbHeight = baseHeight * currentScale
        val thumbWidthPx = with(LocalDensity.current) { animatedThumbWidth.toPx() }

        // Guard against NaN or infinite values
        val safeValue = if (value.isNaN() || value.isInfinite()) 0f else value.coerceIn(0f, 1f)
        
        // precise centering with animated width
        val centerPos = totalWidth * safeValue
        
        // Ensure thumbWidthPx is valid
        val safeThumbWidthPx = if (thumbWidthPx.isNaN()) 0f else thumbWidthPx
        
        val offsetX = centerPos - (safeThumbWidthPx / 2)
        
        // Final guard for rounding
        val safeOffsetX = if (offsetX.isNaN()) 0f else offsetX

        // Active Track Alignment
        // [FIX] Extend active track to the CENTER of the thumb (or full cover) to avoid gap
        val activeTrackWidthPx = centerPos // Was offsetX
        val activeTrackWidth = with(LocalDensity.current) { activeTrackWidthPx.toDp() }
        val currentTrackScale = if (effectiveInteracting) 1.5f else 1.0f
        val animatedTrackHeight = baseTrackHeight * currentTrackScale

        GlassTrack(
            modifier = Modifier.fillMaxWidth(),
            currentTrackScale = currentTrackScale,
            activeTrackWidth = activeTrackWidth,
            animatedTrackHeight = animatedTrackHeight
        )

        if (visible) {
             GlassThumb(
                modifier = Modifier
                    .offset { IntOffset(safeOffsetX.roundToInt(), 0) }
                    .align(Alignment.CenterStart),
                width = animatedThumbWidth,
                height = animatedThumbHeight,
                edgeWidth = 0f, // Unused
                distortionStrength = 0f, // Unused
                trackScale = currentTrackScale,
                trackHeight = animatedTrackHeight,
                isGlassEnabled = isGlassEnabled
            )
        }
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
                .clip(RoundedCornerShape(100)) // [FIX] Rounded ends for active track
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
    trackScale: Float,
    trackHeight: Dp,
    isGlassEnabled: Boolean
) {
    // VISUALS REMOVED: Managed by Parent Lens Shader
    // This box is now transparent but occupies space for layout/logic if needed usually.
    // Actually, it's just a marker now.
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .background(Color.Transparent)
    ) {
    }
}
