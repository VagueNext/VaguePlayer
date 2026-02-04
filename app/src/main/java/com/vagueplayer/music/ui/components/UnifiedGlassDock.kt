package com.vagueplayer.music.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun UnifiedGlassDock(
    modifier: Modifier = Modifier,
    blurRadius: Dp = LiquidGlassDefaults.BlurRadius,
    tint: Color = LiquidGlassDefaults.Tint,
    playerContent: @Composable () -> Unit,
    navContent: @Composable (Dp) -> Unit,
    searchContent: @Composable () -> Unit,
    onExpandPlayer: () -> Unit,
    onSearchClick: () -> Unit,
    isSelectionMode: Boolean = false,
    availableWidth: Dp,
    collapseProgress: Float = 0f,
    playerContainerModifier: Modifier = Modifier,
    onPlayerPositioned: (Rect) -> Unit = {},
    onNavPositioned: (Rect) -> Unit = {},
    onSearchPositioned: (Rect) -> Unit = {},
    onExpandDock: () -> Unit = {},
    showNavigation: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp) // Standardized Outer Padding
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxWidth = availableWidth - 24.dp
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Direct calculation - collapseProgress is already animated by physics in MainScreen
            val playerPadding = (48.dp * collapseProgress).coerceAtLeast(0.dp)
            val playerOffsetY = (50.dp * collapseProgress)
            
            // 1. Player Dock (Top Capsule)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                .offset(y = playerOffsetY) 
                .padding(horizontal = playerPadding)
                .height(38.dp)
                .onGloballyPositioned { onPlayerPositioned(it.boundsInRoot()) } // Report Bounds
                .then(playerContainerModifier), 
            contentAlignment = Alignment.Center
            ) {
                // Layer 1: Glass Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(19.dp))
                        .simpleGlass(
                            cornerRadius = 19.dp,
                            enableShader = true
                        )
                )

                // Layer 2: Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(19.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onExpandPlayer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    playerContent()
                }
            }

            // Dynamic Size for Dock Items (50dp -> 38dp)
            val dockItemSize = (50.dp * (1 - collapseProgress) + 38.dp * collapseProgress)
            val dockItemRadius = dockItemSize / 2

            if (showNavigation) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dockItemSize), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Standardized Gap: 12dp
                val expandedNavWidth = maxWidth - 12.dp - dockItemSize 
                val collapsedNavWidth = 38.dp 
                
                val currentNavWidth = (expandedNavWidth.value * (1 - collapseProgress) + collapsedNavWidth.value * collapseProgress).dp

                // Main Nav Pill (Left/Center)
                Box(
                    modifier = Modifier
                        .width(currentNavWidth) 
                        .height(dockItemSize) 
                        .align(Alignment.CenterVertically)
                        .onGloballyPositioned { onNavPositioned(it.boundsInRoot()) }, // Report Bounds
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: GLASS BACKGROUND
                    Box(
                        modifier = Modifier
                            .fillMaxSize() 
                            .clip(RoundedCornerShape(dockItemRadius)) 
                            .simpleGlass(
                                cornerRadius = 100.dp, // Use large radius for pill
                                enableShader = true
                            )
                    )

                    // LAYER 2: CONTENT
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        navContent(expandedNavWidth)
                        
                        // Click Interceptor for Expansion
                        // When collapsed (progress > 0.5), intercept clicks to trigger expansion
                        if (collapseProgress > 0.1f) {
                             Box(
                                 modifier = Modifier
                                    .fillMaxSize()
                                    .bouncyClickable(
                                        targetScale = 0.95f,
                                        onClick = onExpandDock
                                    )
                             )
                        }
                    }
                }

                // Search Orb (Right) - Fixed Size
                Box(
                    modifier = Modifier
                        .size(dockItemSize)
                        .align(Alignment.CenterVertically)
                        .onGloballyPositioned { onSearchPositioned(it.boundsInRoot()) } // Report Bounds
                        .bouncyClickable(
                            targetScale = 0.95f,
                            onClick = onSearchClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: GLASS BACKGROUND
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius))
                            .simpleGlass(
                                cornerRadius = 100.dp,
                                enableShader = true
                            )
                    )

                    // LAYER 2: CONTENT
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        searchContent()
                    }
                }
            }
        }
    }
}
}
