package com.vagueplayer.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.graphics.lerp
import com.vagueplayer.music.ui.animation.AnimationSpecs
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * VaguePlayer Liquid Glass Switch (Refactored)
 * 交互规格:
 * 1. 点击时滑块变大，透明度降低 (变大破出距离是静止时的两倍).
 * 2. 运动到最大时透明度最低，大小最大.
 * 3. 运动结束后恢复原状.
 */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    checkedTrackColor: Color = Color(0xFF34C759),
    uncheckedTrackColor: Color = Color(0xFFE9E9EA)
) {
    // 布局常量
    val trackWidth = 60.dp // Wider track
    val trackHeight = 32.dp 
    val padding = 2.dp
    
    // 动画状态
    val switchProgress = remember { Animatable(if (checked) 1f else 0f) }
    
    // Internal source if not provided
    val finalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    
    LaunchedEffect(checked) {
        switchProgress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
        )
    }

    val progress = switchProgress.value
    
    // 1. Interaction Factor (0 -> 1 -> 0)
    // Peak at 0.5
    val interactionFactor = sin(progress * PI).toFloat().coerceIn(0f, 1f)
    
    // 2. Size Logic (Ellipse)
    // Resting: Slightly wider than height (Ellipse)
    val thumbHeightResting = trackHeight - (padding * 2) // 28dp
    val thumbWidthResting = thumbHeightResting * 1.2f // 33.6dp (Ellipse)
    
    // Growing logic (Aggressive Expansion for Overflow)
    // "Break out of box" -> Make it significantly larger than track
    val widthMax = thumbWidthResting * 1.7f // Much wider
    val heightMax = thumbHeightResting * 1.9f // Taller than track (32dp -> ~53dp)
    
    val currentWidth = lerp(thumbWidthResting, widthMax, interactionFactor)
    val currentHeight = lerp(thumbHeightResting, heightMax, interactionFactor)
    
    // 3. Alpha Logic
    val currentThumbAlpha = lerp(1f, 0.4f, interactionFactor)
    
    // 4. Track Color
    val currentTrackColor = lerp(uncheckedTrackColor, checkedTrackColor, progress)

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .graphicsLayer { clip = false } // Allow thumb to expand beyond track limits
            .clickable(
                indication = null,
                interactionSource = finalInteractionSource,
                enabled = enabled
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(100))
                .background(currentTrackColor)
        )
        
        // Thumb Container
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            // Move from Left Center to Right Center
            // Left Center X: padding + widthResting/2 (visual center at rest)
            // But since thumb is elliptic, let's anchor by CENTER.
            
            // Start State (Unchecked)
            // CenterX should be such that Left Edge = padding
            // CenterX_Start = padding + thumbWidthResting / 2
            
            // End State (Checked)
            // CenterX_End = trackWidth - padding - thumbWidthResting / 2
            
            val combinedStartPadding = padding + (thumbWidthResting / 2)
            val combinedEndPadding = trackWidth - padding - (thumbWidthResting / 2)
            
            val centerX = lerp(combinedStartPadding, combinedEndPadding, progress)
            val centerY = trackHeight / 2 // Vertically centered
            
            // Use requiredSize to bypass parent constraints and allow "breaking the frame"
            // The parent BoxWithConstraints might clip if we don't handle alignment or overflow.
            // We use a centered Box that is allowed to draw outside.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart) // Anchor start
                    .graphicsLayer {
                        translationX = centerX.toPx() - (currentWidth.toPx() / 2)
                        // Vertical Centering handled by Alignment.CenterStart
                        // Previous manual calculation was adding an offset (centerY - height/2) which shifted it down.
                        translationY = 0f
                        
                        // Disable clipping here too
                        clip = false 
                    }
                    .requiredSize(width = currentWidth, height = currentHeight) // Force size, ignoring constraints
                    .zIndex(10f)
            ) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                         // Ellipse Shape
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(100), spotColor = Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = currentThumbAlpha))
                )
            }
        }
    }
}
