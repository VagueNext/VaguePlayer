package com.vagueplayer.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun MorphingGlassMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    anchorSize: DpSize? = null, // Width/Height of the anchor button
    expandUp: Boolean = false, // Direction (default Down)
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!isExpanded) return

    // Constraint: Popup must be offset to avoid clipping for negative coordinates (expandUp)
    // We calculate the animation values first, then pass the current offset to the Popup.

    // 0. Capture Global Position of Anchor (Internal)
    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Layout(content = {}, modifier = Modifier.onGloballyPositioned { parentCoordinates = it }) { _, _ -> layout(0,0){} }

    // 1. Animation State
    var targetSize by remember { mutableStateOf(Size.Zero) }
    var isMenuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(targetSize) {
        if (targetSize.width > 0) isMenuVisible = true
    }

    val transition = updateTransition(targetState = isMenuVisible, label = "MenuMorph")

    // 2. Geometry & Spring Logic
    val density = LocalDensity.current
    val startWidth = with(density) { anchorSize?.width?.toPx() ?: 0f }
    val startHeight = with(density) { anchorSize?.height?.toPx() ?: 0f }

    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Target X Logic:
    // 1. Calculate relative center: (AnchorW - MenuW) / 2
    val relativeCenterX = (startWidth - targetSize.width) / 2f
    
    // 2. Convert to Global -> Clamp -> Convert back
    val anchorGlobalX = parentCoordinates?.positionInWindow()?.x ?: 0f
    val globalTargetX = anchorGlobalX + relativeCenterX
    
    // 3. Screen Edge Gap: 6dp
    val screenGap = with(density) { 6.dp.toPx() }
    val clampedGlobalX = globalTargetX.coerceIn(
        screenGap, 
        screenWidthPx - targetSize.width - screenGap
    )
    
    // 4. Final Relative Target X
    val targetX = if (parentCoordinates != null) {
        clampedGlobalX - anchorGlobalX
    } else {
        relativeCenterX // Fallback if layout not ready
    }
    
    // Target Y: Up or Down with Gap
    val gap = with(density) { 6.dp.toPx() } // [GAP UPDATED to 6dp]
    val targetY = if (expandUp) {
            -targetSize.height - gap
    } else {
            startHeight + gap
    }

    // Spring Config
    val popSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 350f)

    // Animated Values
    val offsetX by transition.animateFloat(transitionSpec = { popSpring }, label = "X") { open ->
        if (open) targetX else 0f
    }
    val offsetY by transition.animateFloat(transitionSpec = { popSpring }, label = "Y") { open ->
        if (open) targetY else 0f
    }
    val width by transition.animateFloat(transitionSpec = { popSpring }, label = "Width") { open ->
        if (open) targetSize.width else startWidth
    }
    val height by transition.animateFloat(transitionSpec = { popSpring }, label = "Height") { open ->
        if (open) targetSize.height else startHeight
    }
    
    // Corners
    val startRadius = startHeight / 2f
    val endRadius = with(density) { 16.dp.toPx() }
    val cornerRadius by transition.animateFloat(transitionSpec = { popSpring }, label = "Corner") { open ->
        if (open) endRadius else startRadius
    }
    
    // Opacity
    val contentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200, delayMillis = 100) }, 
        label = "Content"
    ) { open -> if (open) 1f else 0f }

    // 3. Render Popup
    // Key Fix: Apply offset to the Popup itself so the Window moves to the draw area (preventing clipping)
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, excludeFromSystemGesture = true),
        offset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
    ) {
        Layout(
            content = {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(4.dp),
                    content = content
                )
            },
            modifier = Modifier
                // Offset removed here (handled by Popup)
                .waterDropGlass(
                    hazeState = hazeState,
                    cornerRadius = with(density) { cornerRadius.toDp() },
                    blurRadius = 40.dp,
                    edgeWidth = 8.0f,
                    distortionStrength = 8.0f,
                    tint = Color.White.copy(alpha = 0.35f),
                    enableShader = true
                )
        ) { measurables, _ ->
            // Measure naturally
            val looseConstraints = androidx.compose.ui.unit.Constraints()
            val placeable = measurables[0].measure(looseConstraints)
            
            val naturalWidth = placeable.width.toFloat()
            val naturalHeight = placeable.height.toFloat() 
            
            // Update Target Size (Loop Prevention)
            if (kotlin.math.abs(targetSize.width - naturalWidth) > 1f || 
                kotlin.math.abs(targetSize.height - naturalHeight) > 1f) {
                targetSize = Size(naturalWidth, naturalHeight)
            }

            val animatedW = width.roundToInt().coerceAtLeast(1)
            val animatedH = height.roundToInt().coerceAtLeast(1)

            layout(animatedW, animatedH) {
                placeable.placeWithLayer(0, 0) {
                    alpha = contentAlpha
                }
            }
        }
    }
}
