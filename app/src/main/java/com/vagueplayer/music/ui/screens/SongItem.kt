package com.vagueplayer.music.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.viewmodel.AudioViewModel
import com.vagueplayer.music.ui.components.bouncyClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot

// Helper Composable for Search Results to keep MainScreen clean
@Composable
fun SongItem(
    song: com.vagueplayer.music.data.model.Song,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    viewModel: com.vagueplayer.music.viewmodel.AudioViewModel,
    matchType: String? = null,
    onClick: ((com.vagueplayer.music.data.model.Song) -> Unit)? = null,
    onLongClick: ((com.vagueplayer.music.data.model.Song) -> Unit)? = null, // [NEW] Long Click Handler
    onMenuClick: ((com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset) -> Unit)? = null // [UPDATED] Pass Offset
) {
    val isSelected = selectedIds.contains(song.id)
    var menuAnchor by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .bouncyClickable(
                targetScale = 0.98f,
                onLongClick = { onLongClick?.invoke(song) }, // [NEW] Pass long click
                onClick = {
                    if (isSelectionMode) {
                        viewModel.toggleSelection(song.id)
                    } else if (onClick != null) {
                        onClick(song)
                    } else {
                        viewModel.playSong(song, emptyList(), "Search: $matchType") 
                    }
                }
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        coil.compose.AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = androidx.compose.ui.Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_crop)
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.size(12.dp))
        androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                text = song.title, 
                fontSize = 17.sp, 
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = if (isSelected && isSelectionMode) com.vagueplayer.music.ui.theme.AccentBlue else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f), 
                maxLines = 1
            )
            androidx.compose.material3.Text(
                text = song.artist, 
                fontSize = 12.sp, 
                color = androidx.compose.ui.graphics.Color.Gray, 
                maxLines = 1
            )
        }
        
        if (isSelectionMode) {
            androidx.compose.material3.RadioButton(
                selected = isSelected,
                onClick = { viewModel.toggleSelection(song.id) },
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = com.vagueplayer.music.ui.theme.AccentBlue,
                    unselectedColor = androidx.compose.ui.graphics.Color.Gray
                )
            )
        } else {
            IconButton(
                onClick = { onMenuClick?.invoke(song, menuAnchor) },
                modifier = Modifier.onGloballyPositioned {
                    // Capture center position of the button
                    val bounds = it.boundsInRoot()
                    menuAnchor = androidx.compose.ui.geometry.Offset(bounds.left, bounds.bottom)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}
