package com.vagueplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex // [FIX] Added import
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect // [FIX] Added import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.layout.onGloballyPositioned // [FIX] Added import
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.ui.components.simpleGlass // [FIX] Import
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle

/**
 * A reusable iOS-style Glass Dialog component.
 * Reference: Apple Vision Pro / iOS Glass Alert.
 */
@Composable
fun GlassDialog(
    blurRadius: androidx.compose.ui.unit.Dp = LiquidGlassDefaults.BlurRadius,
    tint: androidx.compose.ui.graphics.Color = LiquidGlassDefaults.Tint,
    enableShader: Boolean = true, // Kept for API compatibility, though shader unused
    hazeState: HazeState? = null, // [NEW] HazeState for background blur
    onDismissRequest: () -> Unit,
    title: String,
    description: String? = null,
    icon: ImageVector? = null, 
    content: @Composable (() -> Unit)? = null, 
    confirmText: String? = "Confirm",
    onConfirm: (() -> Unit)? = null,
    cancelText: String? = "Cancel",
    onCancel: () -> Unit = onDismissRequest,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null // [NEW] Unified Lens Support
) {
    // [FIX] Inline Dialog to allow Glass Refraction (RenderEffect requires same Window)
    androidx.activity.compose.BackHandler(onBack = onDismissRequest)

    // [RESTORED] Use hazeChild to blur the background content from the global haze source
    // The global .haze() in MainScreen provides the source content

    Box(
        modifier = Modifier
            .zIndex(200f) // Ensure it sits above other content
            .fillMaxSize()
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Color.White,
                            tint = dev.chrisbanes.haze.HazeTint(
                                color = Color.Black.copy(alpha = 0.4f) // [FIX] 40% Tint on Background
                            ),
                            blurRadius = 10.dp, // [FIX] 10% Blur on Background
                            noiseFactor = 0f
                        )
                    )
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.4f))
                }
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
            // The Glass Card
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(32.dp)) 
                    .clickable(enabled = false) {}
            ) {
                // 1. BACKGROUND LAYER
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .onGloballyPositioned { onLayoutCoordinates?.invoke(it) } // [NEW] Report bounds
                        .simpleGlass(
                            cornerRadius = 32.dp,
                            distortionStrength = 40f, 
                            edgeWidth = 5f // [FIX] Reduced edge width to prevent white rim
                        )
                )

                // 2. CONTENT LAYER
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Title & Description
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.Black,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        if (description != null) {
                            Text(
                                text = description,
                                color = Color.Black.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Content
                    if (content != null) {
                        Box(modifier = Modifier.padding(vertical = 8.dp)) {
                            content()
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (cancelText != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(100)) 
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .clickable { onCancel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cancelText,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        if (onConfirm != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(100)) 
                                    .background(AccentBlue)
                                    .clickable { onConfirm() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = confirmText ?: "Confirm",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
}

/**
 * Custom Sleep Timer Dialog
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerDialog(
    currentTimerMin: Int?, 
    hazeState: HazeState? = null, // [FIX] Added HazeState
    onSetTimer: (Int?) -> Unit,
    onDismiss: () -> Unit
) {

    // Options: 10, 15, 30
    val options = listOf(10, 15, 30)
    var customInput by remember { mutableStateOf(currentTimerMin?.toString() ?: "") }
    
    // Derived state for button text
    val isTimerSet = customInput.toIntOrNull() != null && customInput.toIntOrNull()!! > 0

    GlassDialog(
        hazeState = hazeState, // [FIX] Pass HazeState
        title = "睡眠定时",
        description = "自动停止播放...",
        icon = Icons.Default.Timer,
        onDismissRequest = onDismiss,
        confirmText = if (isTimerSet) "开始" else if (currentTimerMin != null) "关闭定时" else null, // Hide if no action
        onConfirm = if (isTimerSet || currentTimerMin != null) {
            {
                val minutes = customInput.toIntOrNull()
                if (minutes != null && minutes > 0) {
                    onSetTimer(minutes)
                } else {
                    onSetTimer(null) // Turn off if active and input empty/0
                }
                onDismiss()
            }
        } else null, // Hide if no action
        cancelText = "取消",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Custom Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                   if (customInput.isEmpty()) {
                        Text(
                            text = "自定义时长 (分钟)",
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = customInput,
                        onValueChange = { input ->
                            // Validate: Digits only, Max 180
                            if (input.all { it.isDigit() }) {
                                val num = input.toIntOrNull()
                                if (num == null || num <= 180) {
                                    customInput = input
                                } else {
                                    customInput = "180"
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // 2. Preset Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly 
                ) {
                   options.forEach { min ->
                       val isSelected = customInput == min.toString()
                       Box(
                           modifier = Modifier
                               .weight(1f)
                               .padding(horizontal = 4.dp)
                               .clip(RoundedCornerShape(12.dp))
                               .background(
                                   if (isSelected) AccentBlue else Color.Black.copy(alpha = 0.05f)
                               )
                               .clickable { customInput = min.toString() }
                               .padding(vertical = 12.dp),
                           contentAlignment = Alignment.Center
                       ) {
                           Text(
                               "$min m",
                               color = if (isSelected) Color.White else Color.Black,
                               fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                           )
                       }
                   }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

/**
 * Simplified Glass Alert Dialog (Confirm/Cancel).
 */
@Composable
fun GlassAlertDialog(
    title: String,
    description: String,
    icon: ImageVector? = null,
    confirmText: String = "确定",
    cancelText: String = "取消",
    hazeState: HazeState? = null, // [NEW] Haze support
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null // [NEW] Unified Lens Support
) {
    GlassDialog(
        title = title,
        description = description,
        icon = icon,
        confirmText = confirmText,
        cancelText = cancelText,
        hazeState = hazeState, // [NEW] Forward
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        onLayoutCoordinates = onLayoutCoordinates // [NEW] Forward
    )
}

/**
 * Simplified Glass Input Dialog (TextField).
 */
@Composable
fun GlassInputDialog(
    title: String,
    description: String? = null,
    initialValue: String = "",
    placeholder: String = "",
    icon: ImageVector? = null,
    confirmText: String = "保存",
    cancelText: String = "取消",
    hazeState: HazeState? = null, // [FIX] Added HazeState
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    GlassDialog(
        hazeState = hazeState, // [FIX] Pass HazeState
        title = title,
        description = description,
        icon = icon,
        confirmText = confirmText,
        cancelText = cancelText,
        onConfirm = { onConfirm(text) },
        onDismissRequest = onDismiss,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
