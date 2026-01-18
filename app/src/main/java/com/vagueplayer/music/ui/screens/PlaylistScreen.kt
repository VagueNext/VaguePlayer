package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background // [FIX]
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable // [FIX] Import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // [FIX]
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vagueplayer.music.viewmodel.AudioViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import dev.chrisbanes.haze.HazeState
import com.vagueplayer.music.ui.theme.AccentBlue

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistScreen(
    onCreatePlaylist: () -> Unit,
    onShowAddMenu: (androidx.compose.ui.geometry.Offset) -> Unit,
    hazeState: HazeState? = null, // Deprecated usage but kept for signature compatibility if needed
    onPlaylistClick: (Playlist) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val viewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(context))
    val playlists by viewModel.userPlaylists.collectAsState()
    
    // Management States
    // isManageMode removed - using long press for delete instead
    var showDeleteDialog by remember { mutableStateOf(false) }
    var playlistForAction by remember { mutableStateOf<Playlist?>(null) }

    // Root Container
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header with Manage/Add Actions
            com.vagueplayer.music.ui.components.ScreenHeader(
                title = "歌单",
                action = {
                    Box {
                        var localBtnPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                        
                        // [FIX] Manage Toggle Removed
                        
                        // Add Button
                        IconButton(
                            onClick = { onShowAddMenu(localBtnPos) },
                            modifier = Modifier.onGloballyPositioned { 
                                localBtnPos = it.boundsInWindow().topLeft
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = AccentBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            )

            // Playlist Grid (New Design)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { 
                            onPlaylistClick(playlist) 
                        },
                        onLongClick = {
                             playlistForAction = playlist
                             showDeleteDialog = true
                        },
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }

        // Delete Dialog
        if (showDeleteDialog && playlistForAction != null) {
            com.vagueplayer.music.ui.components.GlassAlertDialog(
                hazeState = hazeState,
                title = "删除歌单",
                description = "确定要删除歌单 '${playlistForAction!!.name}' 吗？",
                icon = Icons.Default.Delete,
                confirmText = "删除",
                onConfirm = {
                    playlistForAction?.let { viewModel.deletePlaylist(it.id) }
                    showDeleteDialog = false
                    playlistForAction = null
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class) // [FIX] Added ExpFoundation
@Composable
fun SharedTransitionScope.PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit, // [NEW]
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Vertical Card Layout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable( // [FIX] Use combinedClickable
                onClick = onClick,
                onLongClick = onLongClick
             )
    ) {
        // 1. Container/Image Area
        // In the Grid Design, the "Shared Container" is effectively the Image Area
        // We wrap the image in a Box to hold the Delete Badge and apply the Container Transform
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                // Shared Bounds (Container Transform)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "container_${playlist.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray.copy(alpha = 0.2f)) // Subtle placeholder bg
        ) {
            // Shared Element (Image Flight)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playlist.songs.firstOrNull()?.albumArtUri)
                    .crossfade(true)
                    .memoryCacheKey("cover_${playlist.id}")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = "image_${playlist.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Text Info (Outside Shared Bounds for cleaner text fade)
        Text(
            text = playlist.name,
            fontSize = 17.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "title_${playlist.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds() // [FIX] Added ()
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${playlist.songs.size} 首歌曲", // Subtitle
            fontSize = 14.sp,
            color = Color.Gray,
            maxLines = 1
        )
    }
}
