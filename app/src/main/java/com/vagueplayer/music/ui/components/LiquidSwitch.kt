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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vagueplayer.music.ui.animation.AnimationSpecs
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState

/**
 * VaguePlayer Liquid Glass Switch
 * 使用统一的 AnimationSpecs.GlassDeformation 系统
 */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    checkedTrackColor: Color = Color(0xFF34C759), // Apple Green
    uncheckedTrackColor: Color = Color(0xFFE9E9EA)
) {
    // =========================================================================
    // 统一动画规格 (Unified Animation Specs)
    // =========================================================================
    
    // 位置动画 - 使用 ElasticSnappy (快速响应)
    val positionSpec = AnimationSpecs.ElasticSnappy
    
    // 形变动画 - 使用 ElasticJelly (液态回弹)
    val jellySpec = AnimationSpecs.ElasticJelly
    
    // 颜色动画 - 使用 ColorSpring
    val colorSpec = AnimationSpecs.ColorSpring

    // =========================================================================
    // 动画状态 (Animated States)
    // =========================================================================
    
    // Track Color
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = colorSpec,
        label = "TrackColor"
    )

    // Thumb Position
    val alignmentBias by animateFloatAsState(
        targetValue = if (checked) 1f else -1f,
        animationSpec = positionSpec,
        label = "ThumbAlign"
    )

    // Thumb Dimensions (Scaled for 38dp Root Height)
    // Root: 40->38 (95%)
    // Checked Width: 54->51, Height: 32->30
    // Unchecked Width: 33->31, Height: 20->19
    val thumbWidth by animateDpAsState(
        targetValue = if (checked) 51.dp else 31.dp,
        animationSpec = AnimationSpecs.DpSpring,
        label = "ThumbWidth"
    )
    
    val thumbHeight by animateDpAsState(
        targetValue = if (checked) 30.dp else 19.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f), // Jelly
        label = "ThumbHeight"
    )

    // Thumb Offset (Horizontal Overflow)
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) 5.dp else 0.dp,
        animationSpec = AnimationSpecs.DpSpring,
        label = "ThumbOffset"
    )

    // Glass Properties (Synced with Unified Framework)
    val edgeWidth by animateFloatAsState(
        targetValue = if (checked) 25.0f else 0f, // [USER REQUEST] Increased distortion range
        animationSpec = jellySpec,
        label = "EdgeWidth"
    )
    
    val distortion by animateFloatAsState(
        targetValue = if (checked) LiquidGlassDefaults.DistortionStrength else 0f, 
        animationSpec = jellySpec,
        label = "Distortion"
    )

    // Thumb Tint Animation (White -> Fully Transparent)
    val thumbTint by animateColorAsState(
        targetValue = if (checked) Color.White.copy(alpha = 0.0f) else Color.White,
        animationSpec = colorSpec,
        label = "ThumbTint"
    )

    // =========================================================================
    // 布局 (Layout) - 38dp Height
    // =========================================================================
    
    val trackWidth = 57.dp // 60 * 0.95
    val trackHeight = 21.dp // 22 * 0.95
    val rootHeight = 38.dp // Requested Height 

    val localHazeState = remember { HazeState() }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = rootHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Track Layer (Source for Glass)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(localHazeState), // Capture Track
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = trackWidth, height = trackHeight)
                    .clip(RoundedCornerShape(100))
                    .background(trackColor)
            )
        }

        // Thumb Layer (Glass Effect Ready)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = alignmentBias, verticalBias = 0f))
                    .offset(x = thumbOffsetX)
                    .zIndex(10f)
                    .size(width = thumbWidth, height = thumbHeight)
                    .shadow(
                        elevation = if (checked) 8.dp else 2.dp,
                        shape = CircleShape,
                        spotColor = Color(0x20000000)
                    )
                    // Enable waterDropGlass for the thumb
                    .waterDropGlass(
                        hazeState = localHazeState, // Use Local Haze to refract Track 
                        cornerRadius = thumbHeight / 2,
                        blurRadius = 0.dp,
                        tint = thumbTint, // Animated Tint
                        edgeWidth = edgeWidth,
                        distortionStrength = distortion,
                        enableShader = true
                    )
            )
        }
    }
}
