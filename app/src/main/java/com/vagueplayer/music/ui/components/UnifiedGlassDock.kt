package com.vagueplayer.music.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun UnifiedGlassDock(
    modifier: Modifier = Modifier,
    hazeState: HazeState?,
    blurRadius: Dp = LiquidGlassDefaults.BlurRadius,
    tint: Color = LiquidGlassDefaults.Tint,
    edgeWidth: Float = LiquidGlassDefaults.EdgeWidth,
    distortionStrength: Float = LiquidGlassDefaults.DistortionStrength,
    playerContent: @Composable () -> Unit,
    navContent: @Composable () -> Unit,
    searchContent: @Composable () -> Unit,
    onExpandPlayer: () -> Unit,
    onSearchClick: () -> Unit,
    isSelectionMode: Boolean = false, // New parameter
    collapseProgress: Float = 0f // [NEW] 0f = Expanded, 1f = Collapsed
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 20.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxWidth = maxWidth
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real-time Glass Engine
            val infiniteTransition = rememberInfiniteTransition(label = "glass_engine")
            val time by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "glass_ticker"
            )

            // 1. Player Dock (Top Capsule)
            // Animation: Shrink width by adding padding
            // [RESIZE] Updated padding to 48dp (User Request)
            val playerPadding by animateDpAsState(
                targetValue = (48.dp * collapseProgress).coerceAtLeast(0.dp), 
                label = "PlayerPadding"
            )
            
            // [NEW] Vertical Shift: Move down when collapsing
            // Target 54dp = 38dp (Player Height) + 16dp (Spacer) to align centers
            val playerOffsetY by animateDpAsState(
                targetValue = (54.dp * collapseProgress), 
                label = "PlayerOffsetY"
            )
            
            // 1. Player Dock (Top Capsule) - Always Visible (User Request)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = playerOffsetY) // [ANIMATION] Smooth Drop Down
                    .padding(horizontal = playerPadding) // [ANIMATION] Shrink effect
                    .height(38.dp), // [RESIZE] Compact Player
                contentAlignment = Alignment.Center
            ) {
                // MERGED LAYER: CONTENT INSIDE GLASS
                // Applying waterDropGlass to the container ensures the content inside 
                // is subject to the RenderEffect (Distortion) at the edges.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(19.dp)) // [RESIZE] Radius = Height / 2
                        .waterDropGlass(
                            hazeState = hazeState, 
                            cornerRadius = 19.dp,
                            blurRadius = blurRadius, 
                            tint = tint, 
                            edgeWidth = edgeWidth, 
                            distortionStrength = distortionStrength, 
                            enableShader = true,
                            time = time
                        )
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

            // 2. Nav Dock Row
            // Animation: Nav Pill shrinks from (MaxWidth - 80dp) to 38dp (Circle)
            // Height: Shrinks from 50dp to 38dp
            
            // [ANIMATION] Dynamic Size for Dock Items (50dp -> 38dp)
            val dockItemSize by animateDpAsState(
                targetValue = (50.dp * (1 - collapseProgress) + 38.dp * collapseProgress),
                label = "DockItemSize"
            )
            val dockItemRadius = dockItemSize / 2

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // Keep apart
                verticalAlignment = Alignment.CenterVertically
            ) {
                val expandedNavWidth = maxWidth - 16.dp - dockItemSize // Total - Spacing - Search
                val collapsedNavWidth = 38.dp // [RESIZE] Target Circle Diameter
                
                // Linear Interpolation for Width
                val currentNavWidth = (expandedNavWidth.value * (1 - collapseProgress) + collapsedNavWidth.value * collapseProgress).dp

                // Main Nav Pill (Left/Center)
                Box(
                    modifier = Modifier
                        .width(currentNavWidth) // [ANIMATION] Dynamic Width
                        .height(dockItemSize), // [ANIMATION] Dynamic Height
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: GLASS BACKGROUND
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Fills the animated width
                            .clip(RoundedCornerShape(dockItemRadius)) // [ANIMATION] Dynamic Radius
                            .waterDropGlass(
                                hazeState = hazeState, 
                                cornerRadius = dockItemRadius,
                                blurRadius = blurRadius, 
                                tint = tint, 
                                edgeWidth = edgeWidth, 
                                distortionStrength = distortionStrength, 
                                enableShader = true, 
                                time = time
                            )
                    )

                    // LAYER 2: CONTENT
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}, 
                        contentAlignment = Alignment.Center
                    ) {
                        navContent()
                    }
                }

                // Search Orb (Right) - Fixed Size
                Box(
                    modifier = Modifier
                        .size(dockItemSize), // [ANIMATION] Dynamic Size
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: GLASS BACKGROUND
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius))
                            .waterDropGlass(
                                hazeState = hazeState, 
                                cornerRadius = dockItemRadius,
                                blurRadius = blurRadius, 
                                tint = tint, 
                                edgeWidth = edgeWidth, 
                                distortionStrength = distortionStrength, 
                                enableShader = true, 
                                time = time
                            )
                    )

                    // LAYER 2: CONTENT
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dockItemRadius))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSearchClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        searchContent()
                    }
                }
            }
        }
    }
}
