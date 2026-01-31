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

@Composable
fun PlaylistActionMenu(
    isExpanded: Boolean,
    anchorSize: androidx.compose.ui.unit.DpSize, // Made required
    anchorPosition: androidx.compose.ui.geometry.Offset,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onAddPlaylist: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    MorphingGlassMenu(
        isExpanded = isExpanded,
        anchorSize = anchorSize,
        anchorPosition = anchorPosition, // Forwarded
        onLayoutCoordinates = onLayoutCoordinates, // Forwarded
        hazeState = hazeState,
        onDismiss = onDismiss,
        expandUp = false // Default
    ) {
        // Content
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
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
            .bouncyClickable(
                targetScale = 0.98f,
                onClick = onClick
            )
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
