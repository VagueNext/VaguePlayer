package com.vagueplayer.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.data.model.Song

@Composable
fun SongActionMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    anchorPosition: Offset,
    song: Song?,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    anchorSize: androidx.compose.ui.unit.DpSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
    onLayoutCoordinates: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {},
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    if (song == null) return

    MorphingGlassMenu(
        isExpanded = isExpanded,
        onDismiss = onDismiss,
        anchorSize = anchorSize,
        anchorPosition = anchorPosition,
        onLayoutCoordinates = onLayoutCoordinates,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(vertical = 8.dp)
        ) {
            // Option 1: Add to Next
            MenuOption(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = "加入播放列表",
                onClick = {
                    onPlayNext(song)
                    onDismiss()
                }
            )

            // Option 2: Add to Playlist
            MenuOption(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = "加入歌单",
                onClick = {
                    onAddToPlaylist(song)
                    onDismiss()
                }
            )

            // Option 3: Delete (Soft)
            MenuOption(
                icon = Icons.Default.Delete,
                label = "从列表删除",
                color = Color.Red.copy(alpha = 0.8f),
                onClick = {
                    onDelete(song)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun MenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = color
        )
    }
}
