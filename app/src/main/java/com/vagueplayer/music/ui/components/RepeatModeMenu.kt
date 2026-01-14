package com.vagueplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.vagueplayer.music.ui.components.RoundedRepeatIcon
import com.vagueplayer.music.ui.components.RoundedShuffleIcon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer // Using Timer as placeholder for "Set Count"
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import dev.chrisbanes.haze.HazeState

@Composable
fun RepeatModeMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    anchorSize: androidx.compose.ui.unit.DpSize, // Made required
    anchorPosition: androidx.compose.ui.geometry.Offset, // [NEW] Explicit Anchor Position
    currentMode: Int,
    onModeSelected: (Int) -> Unit,
    onSetCount: () -> Unit,
    hazeState: HazeState? = null
) {
    MorphingGlassMenu(
        isExpanded = isExpanded,
        onDismiss = onDismiss,
        anchorSize = anchorSize,
        anchorPosition = anchorPosition, // Forwarded
        expandUp = true,
        hazeState = hazeState
    ) {
        // Content
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Shuffle
            MenuItem(
                icon = null,
                label = "随机播放",
                isSelected = currentMode == 3, 
                onClick = { onModeSelected(3) },
                customIcon = {
                     RoundedShuffleIcon(color = Color.Black, modifier = Modifier.size(16.dp))
                }
            )
            
            // 2. Loop (Repeat All)
            MenuItem(
                icon = null,
                label = "循环播放",
                isSelected = currentMode == Player.REPEAT_MODE_ALL,
                onClick = { onModeSelected(Player.REPEAT_MODE_ALL) },
                customIcon = {
                     RoundedRepeatIcon(count = null, color = Color.Black, modifier = Modifier.size(16.dp))
                }
            )
            
            // 3. Single Loop (Repeat One)
            MenuItem(
                icon = null,
                label = "单曲循环",
                isSelected = currentMode == Player.REPEAT_MODE_ONE,
                onClick = { onModeSelected(Player.REPEAT_MODE_ONE) },
                customIcon = {
                     RoundedRepeatIcon(count = "1", color = Color.Black, modifier = Modifier.size(16.dp))
                }
            )
            
            // 4. Set Count
            MenuItem(
                icon = null, // Custom Icon
                label = "设置次数",
                isSelected = false,
                onClick = { onSetCount() },
                customIcon = {
                    RoundedRepeatIcon(
                        count = "N",
                        color = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    customIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent) 
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black, 
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = Color.Black 
        )
    }
}
