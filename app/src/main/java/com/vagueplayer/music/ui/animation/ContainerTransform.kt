package com.vagueplayer.music.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset 
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import com.vagueplayer.music.ui.theme.AnimationUtils

/**
 * A unified framework for "Container Transform" (Expandable Floating Layer) animations.
 * Implements the Material Design / iOS App Library style expansion with Spring physics.
 */

/**
 * The "Destination" or "Target" of the container transform.
 * Handles the Scrim, the Expanded Card, and the Physics.
 *
 * @param isExpanded Whether the container is currently expanded.
 * @param key The unique key shared between Source and Target.
 * @param onDismissRequest Called when the scrim or background is clicked.
 * @param alignment Where to position the expanded content (default: Center).
 * @param containerColor Background color of the expanded card.
 * @param scrimColor Color of the background dimming layer.
 * @param cornerRadius The corner radius of the expanded card (Source radius is determined by Source modifier).
 * @param modifier Modifier applied to the expanded card itself (e.g. width, height).
 * @param content The content inside the expanded card.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ExpandableContainer(
    isExpanded: Boolean,
    key: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    containerColor: Color = Color.White,
    scrimColor: Color = Color.Black.copy(alpha = 0.3f),
    cornerRadius: Dp = 28.dp, // Default "Big" corner radius
    renderInOverlay: Boolean = true, // Control parameter
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    // 1. Scrim (Removed as per user request)
    // Was: Lines 62-78
    // 2. Expanded Card (Target)
    AnimatedVisibility(
        visible = isExpanded,
        enter = EnterTransition.None, 
        exit = ExitTransition.None,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f) 
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Scrim (Fades out as we drag)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                         interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                         indication = null
                    ) { 
                        // Tap scrim to dismiss
                        onDismissRequest() 
                    }
            ) {}


            // We use 'this' which is the AnimatedVisibilityScope from the surrounding AnimatedVisibility block
            val currentScope = this@AnimatedVisibility
            val sharedScope = this@ExpandableContainer
            // Hoist Composable call to strictly Composable scope
            val sharedState = rememberSharedContentState(key = key)
            
            val sharedElementModifier: androidx.compose.ui.Modifier = with(sharedScope) {
                androidx.compose.ui.Modifier.sharedBounds(
                    sharedContentState = sharedState,
                    animatedVisibilityScope = currentScope,
                    boundsTransform = { _, _ -> AnimationUtils.sharedElementSpring },
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    renderInOverlayDuringTransition = renderInOverlay 
                )
            }


            
            // Container Content (The Card)
            Box(
                modifier = androidx.compose.ui.Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth() 
                    .fillMaxHeight() 
                    .then(sharedElementModifier)
                    .background(containerColor, androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)) 
                    .graphicsLayer {
                        this.clip = true
                        this.shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius) // Simplify radius
                    }
                    // Draggable removed to prevent conflicts with PlayerScreen's own gesture handling
            ) {
                 content()
                 
                 // Close Button?
            }
        }
    }
}

/**
 * Modifier extension to easily tag a Composable as the "Source" of a Container Transform.
 *
 * @param key The unique key matching the Target.
 * @param sharedTransitionScope The global SharedTransitionScope.
 * @param animatedVisibilityScope The local AnimatedVisibilityScope (e.g. from AnimatedContent or AnimatedVisibility).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.transformSource(
    key: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    renderInOverlay: Boolean = false // Default to FALSE to prevent LazyGrid crashes globally
): Modifier = with(sharedTransitionScope) {
    this@transformSource.sharedBounds(
        sharedContentState = rememberSharedContentState(key = key),
        animatedVisibilityScope = animatedVisibilityScope,
        boundsTransform = { _, _ -> AnimationUtils.sharedElementSpring },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        renderInOverlayDuringTransition = renderInOverlay
    )
}
