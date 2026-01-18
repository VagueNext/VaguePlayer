package com.vagueplayer.music.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer // Checked Import
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle // Checked Import
import dev.chrisbanes.haze.HazeTint // Checked Import
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun GlassProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    isGlassEnabled: Boolean = false,
    hazeState: HazeState? = null,
    visible: Boolean = true,
    onLayoutCoordinates: (LayoutCoordinates) -> Unit = {},
    onInteractionChange: (Boolean) -> Unit = {}
) {
    var isInteracting by remember { mutableStateOf(false) }

    // [REVERT] Standard Height
    val trackHeight = 6.dp
    
    val currentHeight by animateDpAsState(
        targetValue = if (isInteracting) 12.dp else 6.dp, 
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "HeightExp"
    )

    LaunchedEffect(isInteracting) {
        onInteractionChange(isInteracting)
    }

    BoxWithConstraints(
        modifier = modifier
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
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalWidthDp = maxWidth 
        val progressWidth = totalWidthDp * value

        // 1. Background Track (Simple Gray)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeight)
                .clip(RoundedCornerShape(100))
                .background(Color.White.copy(alpha = 0.2f))
                .align(Alignment.Center) 
        )

        // 2. Progress Fill (White)
        Box(
            modifier = Modifier
                .width(progressWidth)
                .height(currentHeight)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(100))
                .background(Color.White)
        )

        // 3. Head Removed (User Req: Old Indicator)
        
        // 4. Interaction Knob (Standard White Circle - Always Visible)
        val thumbSize = 16.dp
        
        // Always Visible Knob
        Box(
            modifier = Modifier
                .offset(x = progressWidth - (thumbSize / 2)) 
                .size(thumbSize)
                .align(Alignment.CenterStart)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
