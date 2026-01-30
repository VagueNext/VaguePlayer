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
    navContent: @Composable (Dp) -> Unit, // [FIX] Pass expandedWidth to content
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
    onExpandDock: () -> Unit = {}, // [NEW] Callback for expand click
    showNavigation: Boolean = true // [NEW] Control bottom bar visibility
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp) // [FIX] Standardized Outer Padding (Was 8h/20v)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxWidth = availableWidth - 24.dp // [FIX] 12dp * 2 padding
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // [FIX] Standardized Gap (Was 16dp)
        ) {

            // [FIX] Direct calculation - collapseProgress is already animated by physics in MainScreen
            val playerPadding = (48.dp * collapseProgress).coerceAtLeast(0.dp)
            val playerOffsetY = (50.dp * collapseProgress) // [FIX] 38dp (Height) + 12dp (Gap) = 50dp
            
            // 1. Player Dock (Top Capsule)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                .offset(y = playerOffsetY) 
                .padding(horizontal = playerPadding)
                .height(38.dp)
                .onGloballyPositioned { onPlayerPositioned(it.boundsInRoot()) } // [GLASS] Report Bounds
                .then(playerContainerModifier), 
            contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(19.dp))
                        // Transparent - Shader handles visual
                        .background(Color.Transparent) 
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

            // [ANIMATION] Dynamic Size for Dock Items (50dp -> 38dp)
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
                // [FIX] Standardized Gap: 12dp
                val expandedNavWidth = maxWidth - 12.dp - dockItemSize 
                val collapsedNavWidth = 38.dp 
                
                val currentNavWidth = (expandedNavWidth.value * (1 - collapseProgress) + collapsedNavWidth.value * collapseProgress).dp

                // Main Nav Pill (Left/Center)
                Box(
                    modifier = Modifier
                        .width(currentNavWidth) 
                        .height(dockItemSize) 
                        .align(Alignment.CenterVertically)
                        .onGloballyPositioned { onNavPositioned(it.boundsInRoot()) }, // [GLASS] Report Bounds
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: GLASS BACKGROUND (Transparent now)
                    Box(
                        modifier = Modifier
                            .fillMaxSize() 
                            .clip(RoundedCornerShape(dockItemRadius)) 
                            .background(Color.Transparent)
                    )

                    // LAYER 2: CONTENT
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        navContent(expandedNavWidth) // [FIX] expandedWidth is already Dp
                        
                        // [NEW] Click Interceptor for Expansion
                        // When collapsed (progress > 0.5), intercept clicks to trigger expansion
                        if (collapseProgress > 0.1f) {
                             Box(
                                 modifier = Modifier
                                    .fillMaxSize()
                                    // [FIX] Bouncy Expansion
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
                        .onGloballyPositioned { onSearchPositioned(it.boundsInRoot()) } // [GLASS] Report Bounds
                        // [FIX] Apply Bouncy Click HERE (Parent) so content scales too
                        .bouncyClickable(
                            targetScale = 0.95f,
                            onClick = onSearchClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: GLASS BACKGROUND (Transparent)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius))
                            .background(Color.Transparent)
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

