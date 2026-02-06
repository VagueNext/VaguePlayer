package com.vagueplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import dev.chrisbanes.haze.HazeState
import androidx.compose.material.icons.filled.Timer

/**
 * A reusable iOS-style Glass Dialog component.
 * Rewritten to strictly match [UnifiedGlassDock]'s architecture.
 */
@Composable
fun GlassDialog(
    blurRadius: androidx.compose.ui.unit.Dp = LiquidGlassDefaults.BlurRadius,
    tint: androidx.compose.ui.graphics.Color = LiquidGlassDefaults.Tint,
    enableShader: Boolean = true,
    hazeState: HazeState? = null,
    onDismissRequest: () -> Unit,
    title: String,
    description: String? = null,
    icon: ImageVector? = null, 
    content: @Composable (() -> Unit)? = null, 
    confirmText: String? = "Confirm",
    onConfirm: (() -> Unit)? = null,
    cancelText: String? = "Cancel",
    onCancel: () -> Unit = onDismissRequest,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    // Inline Dialog to allow Glass Refraction
    androidx.activity.compose.BackHandler(onBack = onDismissRequest)

    Box(
        modifier = Modifier
            .zIndex(200f) // Top Layer
            .fillMaxSize()
            // No Scrim (Darkening) as requested, relying on Glass Effect
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        // THE GLASS CARD CONTAINER
        Box(
            modifier = Modifier
                .width(280.dp)
                // Report bounds here for the Global Distortion Lens (MainScreen)
                .onGloballyPositioned { onLayoutCoordinates?.invoke(it) }
                .clickable(enabled = false) {}
        ) {
            // LAYER 1: GLASS BACKGROUND (Visuals)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(32.dp))
                    // [UNIFICATION] Force White Glass everywhere.
                    // Reduces fragmentation between Light/Dark modes. 
                    // Matches UnifiedGlassDock style.
                    .background(Color.White.copy(alpha = 0.40f))
                    .simpleGlass(
                        cornerRadius = 32.dp,
                        enableShader = true 
                    )
            )

            // LAYER 2: CONTENT
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp) // Increased spacing
            ) {
                // Icon
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Text Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    if (description != null) {
                        Text(
                            text = description,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Custom Content Slot
                if (content != null) {
                    Box(modifier = Modifier.padding(vertical = 8.dp)) {
                        content()
                    }
                }

                // Buttons (Transparent Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    if (cancelText != null) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp), // Flexible height
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(cancelText, fontSize = 15.sp)
                        }
                    }

                    // Confirm Button
                    if (onConfirm != null && confirmText != null) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp), // Flexible height
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), // Tinted Primary
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(confirmText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wrapper for simple alerts using GlassDialog (Backward Compatibility)
 */
@Composable
fun GlassAlertDialog(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    confirmText: String? = "Confirm",
    cancelText: String? = "Cancel",
    hazeState: HazeState? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    GlassDialog(
        title = title,
        description = description,
        icon = icon,
        confirmText = confirmText,
        cancelText = cancelText,
        hazeState = hazeState,
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        onLayoutCoordinates = onLayoutCoordinates
    )
}

/**
 * Specialized Sleep Timer Dialog
 */
@Composable
fun SleepTimerDialog(
    hazeState: HazeState? = null,
    currentTimerMin: Int?,
    onSetTimer: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    GlassDialog(
        title = "睡眠定时",
        description = "自动停止播放...",
        hazeState = hazeState,
        icon = androidx.compose.material.icons.Icons.Filled.Timer, // Default icon
        onDismissRequest = onDismiss,
        confirmText = null, // Custom content handles actions
        cancelText = "取消",
        onLayoutCoordinates = onLayoutCoordinates,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset Timers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(10, 15, 30).forEach { mins ->
                        val isSelected = currentTimerMin == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) 
                                )
                                .clickable { 
                                    onSetTimer(mins)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins} m",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                // Custom Input (Simplified for now, or just more presets)
                // For now sticking to presets as reimplementation.
                // Or "Off" button if active
                if (currentTimerMin != null) {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.1f))
                            .clickable { 
                                onSetTimer(null) // Cancel timer
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("关闭定时", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}
