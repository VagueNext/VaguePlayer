package com.vagueplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import dev.chrisbanes.haze.HazeState

@Composable
fun PlaylistActionMenu(
    onAddPlaylist: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    // Compact, "Capsule-like" glass menu
    Box(
        modifier = modifier
            .width(160.dp) 
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(enabled = false) {}, 
        contentAlignment = Alignment.Center
    ) {
        // 1. Background (Glass)
        Box(
            modifier = Modifier
                .matchParentSize()
                .waterDropGlass(
                    hazeState = hazeState,
                    cornerRadius = 16.dp
                )
        )

        // 2. Content
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Add New
            MenuItem(
                icon = Icons.Default.Add,
                label = "添加新歌单",
                onClick = { onAddPlaylist() }
            )
            
            // 2. Import TXT
            MenuItem(
                icon = Icons.Default.FileUpload, // Upload = Import into app
                label = "TXT 导入歌单",
                onClick = { onImportPlaylist() }
            )
            
            // 3. Export TXT
            MenuItem(
                icon = Icons.Default.FileDownload, // Download = Export from app
                label = "TXT 导出歌单",
                onClick = { onExportPlaylist() }
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.8f), 
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black.copy(alpha = 0.9f)
        )
    }
}
