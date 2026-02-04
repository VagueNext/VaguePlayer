package com.vagueplayer.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
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
import com.vagueplayer.music.data.model.Playlist

@Composable
fun PlaylistContextMenu(
    isExpanded: Boolean,
    playlist: Playlist?,
    anchorSize: androidx.compose.ui.unit.DpSize,
    anchorPosition: androidx.compose.ui.geometry.Offset,
    onLayoutCoordinates: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onRename: (Playlist) -> Unit,
    onExport: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    if (playlist == null) return

    MorphingGlassMenu(
        isExpanded = isExpanded,
        anchorSize = anchorSize,
        anchorPosition = anchorPosition,
        onLayoutCoordinates = onLayoutCoordinates,
        hazeState = hazeState,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Rename
            MenuItem(
                icon = Icons.Default.Edit,
                label = "重命名",
                onClick = { onRename(playlist) }
            )
            
            // 2. Export
            MenuItem(
                icon = Icons.Default.Share, 
                label = "导出列表 (TXT)",
                onClick = { onExport(playlist) }
            )
            
            // 3. Delete
            MenuItem(
                icon = Icons.Default.Delete,
                label = "删除歌单",
                onClick = { onDelete(playlist) },
                color = Color.Red.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = Color.Black
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
            tint = color.copy(alpha = 0.8f), 
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.9f)
        )
    }
}
