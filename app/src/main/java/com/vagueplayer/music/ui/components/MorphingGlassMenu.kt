package com.vagueplayer.music.ui.components

import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

 
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned // [FIX] Required for Lens System
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlin.math.roundToInt

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun MorphingGlassMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    anchorSize: DpSize, // Made required/non-nullable for clarity
    anchorPosition: androidx.compose.ui.geometry.Offset, // [NEW] Explicit Anchor Position
    expandUp: Boolean = false,
    hazeState: dev.chrisbanes.haze.HazeState? = null, // [FIX] Add Haze Support
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null, // [NEW] For Lens System
    content: @Composable ColumnScope.() -> Unit
) {

    // We drive the transition with the external isExpanded state directly
    var targetSize by remember { mutableStateOf(Size.Zero) }

    // Decouple visibility from size to ensure animation starts immediately
    val isVisible = isExpanded
    
    val transition = updateTransition(targetState = isVisible, label = "MenuMorph")

    // Opacity for the Whole Menu (Moved up for Exit Logic)
    val menuAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 350) }, 
        label = "MenuAlpha"
    ) { open -> if (open) 1f else 0f }

    // Exit Logic: Keep rendering until Alpha is effectively zero
    if (!isExpanded && menuAlpha < 0.02f) {
        return
    }

    // [FIX] Force a refresh of the glass effect shortly after opening
    var refreshTrigger by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            kotlinx.coroutines.delay(50) 
            refreshTrigger = 1f 
        }
    }
    
    // 2. Geometry & Spring Logic
    val density = LocalDensity.current
    val startWidth = with(density) { anchorSize.width.toPx() }
    val startHeight = with(density) { anchorSize.height.toPx() }

    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Target X Logic:
    val anchorCenterX = anchorPosition.x + (startWidth / 2f)
    val menuTargetLeft = anchorCenterX - (targetSize.width / 2f)
    
    // 2. Clamp
    val screenGap = with(density) { 6.dp.toPx() }
    val clampedLeft = menuTargetLeft.coerceIn(
        screenGap, 
        screenWidthPx - targetSize.width - screenGap
    )
    
    // 3. Final X
    val targetX = clampedLeft
    
    // Target Y
    val gap = with(density) { 6.dp.toPx() }
    val targetY = if (expandUp) {
            anchorPosition.y - targetSize.height - gap
    } else {
            // [FIX] Align Top-Top for "Magnification" effect (Button morphs into Menu)
            // Removed startHeight + gap offset to ensure start position covers the button exactly
            anchorPosition.y 
    }

    // Spring Config
    val popSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 250f)

    // Animated Values
    // When closing (transition.targetState == false && transition.currentState == true),
    // we want to stay at the "Target" (Expanded) values and just fade out (alpha -> 0).
    // So we use (open || transition.currentState) to check if we are in Open or Closing state.
    
    val offsetX by transition.animateFloat(transitionSpec = { popSpring }, label = "X") { open ->
        if (open) targetX else anchorPosition.x 
    }
    val offsetY by transition.animateFloat(transitionSpec = { popSpring }, label = "Y") { open ->
        if (open) targetY else anchorPosition.y 
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
    
    // Content Opacity (Internal elements) - Optional delay for niceness
    val contentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200, delayMillis = 50) }, 
        label = "Content"
    ) { open -> if (open) 1f else 0f }

    // 3. Render Overlay (No Popup)
    // [FIX] Using Box Overlay instead of Popup to ensure shared Window for Haze Transparency
    
    // Full-screen overlay for outside clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f) // Try to float on top if possible (only works if siblings)
            .offset { IntOffset(0, 0) } // Reset any parent offset? No, fillMaxSize takes parent bounds.
    ) {
        // Scrim (Handle dismiss)
        // Only clickable if open? Yes.
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // The Menu Itself
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
                // Apply Global Offset
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer { alpha = menuAlpha } // Fade In/Out Container
                // [FIX] Report coordinates for Global Lens System
                // Only report when expanded and effectively visible to avoid ghost frames
                .onGloballyPositioned { coords ->
                    if (isExpanded && menuAlpha > 0.05f) {
                        onLayoutCoordinates?.invoke(coords)
                    }
                }
                .then(
                    if (hazeState != null) {
                         Modifier
                            .clip(RoundedCornerShape(with(density) { cornerRadius.toDp() }))
                            .hazeChild(
                                state = hazeState, 
                                style = HazeStyle(
                                    blurRadius = 20.dp,
                                    tint = HazeTint(Color.White.copy(alpha = 0.4f)),
                                    noiseFactor = 0.05f
                                )
                            )
                    } else {
                         Modifier.simpleGlass(
                             cornerRadius = with(density) { cornerRadius.toDp() },
                             distortionStrength = 30f 
                        )
                    }
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
