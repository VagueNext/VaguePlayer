package com.vagueplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.spring
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.data.model.Song
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.viewmodel.AudioViewModel
import com.vagueplayer.music.viewmodel.AudioViewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot

import dev.chrisbanes.haze.HazeState
import com.vagueplayer.music.ui.components.GlassDialog
import com.vagueplayer.music.ui.components.waterDropGlass
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

// Imports
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.vagueplayer.music.ui.components.PlaylistActionMenu
import android.widget.Toast

import com.vagueplayer.music.ui.animation.transformSource

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistScreen(
    onCreatePlaylist: () -> Unit,
    onShowAddMenu: (androidx.compose.ui.geometry.Offset) -> Unit,
    hazeState: HazeState? = null,
    onPlaylistClick: (Playlist) -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val viewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(context))
    val playlists by viewModel.userPlaylists.collectAsState()
    val allSongs by viewModel.songs.collectAsState()

    // Dialog & UI States
    // showDetailDialog REMOVED - Hoisted
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) } // Keep for logic? No, MainScreen controls selection.
    // Actually, delete/export might still need local selection tracking if they are context menus.
    // Let's keep a local `selectedPlaylistForAction` for Delete/Rename, but Detail is external.
    var playlistForAction by remember { mutableStateOf<Playlist?>(null) } 
    
    var isExportMode by remember { mutableStateOf(false) }
    var playlistToExportId by remember { mutableStateOf<String?>(null) }

    // Management States
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // File Pickers (Keeping existing logic)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importPlaylistFromTxt(it) }
        }
    )
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let { 
                playlistToExportId?.let { id -> viewModel.exportPlaylistToTxt(id, it) }
            }
            isExportMode = false
            playlistToExportId = null
        }
    )

    // Root Box
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // [FIX] Avoid status bar overlap
                .padding(horizontal = 20.dp)
        ) {
            Column {
                // Header
                com.vagueplayer.music.ui.components.ScreenHeader(
                    title = if (isExportMode) "选择导出歌单" else "歌单",
                    action = {
                        Box {
                            var localBtnPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(playlists) { playlist ->
                        val coverSong = viewModel.getMostPlayedSong(playlist)
                        
                        PlaylistCard(
                            playlist = playlist,
                            coverSong = coverSong,
                            onClick = {
                                if (isExportMode) {
                                    playlistToExportId = playlist.id
                                    exportLauncher.launch("${playlist.name}.txt")
                                } else {
                                    onPlaylistClick(playlist)
                                }
                            },
                            onLongClick = {
                                if (!isExportMode) {
                                    playlistForAction = playlist
                                    showDeleteDialog = true
                                }
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }
    
        // Delete Dialog
        if (showDeleteDialog && playlistForAction != null) {
            com.vagueplayer.music.ui.components.GlassAlertDialog(
                hazeState = hazeState,
                title = "删除歌单",
                description = "确定要删除歌单 '${playlistForAction!!.name}' 吗？此操作无法撤销。",
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

        // Rename Dialog
        if (showRenameDialog && playlistForAction != null) {
            com.vagueplayer.music.ui.components.GlassInputDialog(
                hazeState = hazeState,
                title = "重命名歌单(Safe)",
                initialValue = renameText,
                icon = Icons.Default.Edit,
                onConfirm = { newName ->
                    if (newName.isNotBlank()) {
                         playlistForAction?.let { viewModel.renamePlaylist(it.id, newName) }
                    }
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }
}
        


@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    coverSong: Song?, // Passed from outside
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .then(
                 if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                     with(sharedTransitionScope) {
                          Modifier.transformSource(
                              key = "playlist_card_${playlist.id}",
                              sharedTransitionScope = this,
                              animatedVisibilityScope = animatedVisibilityScope,
                                  renderInOverlay = true
                          )
                     }
                 } else Modifier
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // [SHARED ELEMENT SOURCE] Image
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray), // Fallback
            contentAlignment = Alignment.Center
        ) {

            if (coverSong != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverSong.albumArtUri)
                        .crossfade(true)
                        // 🔥【改这里】用 ID 当做唯一身份证！
                        .memoryCacheKey("cover_cache_${playlist.id}") 
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .then(
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        state = rememberSharedContentState(key = "cover_${playlist.id}"), // 🔥 Matched Key
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier
                        )
                        .fillMaxSize()
                )
            } else {
                // Empty State
                Text(
                    text = playlist.name.firstOrNull()?.toString() ?: "?",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // [SHARED ELEMENT SOURCE] Text
        Text(
            text = playlist.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black.copy(alpha = 0.8f),
            maxLines = 1,
            modifier = Modifier
                .then(
                     if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                         with(sharedTransitionScope) {
                             Modifier.sharedElement(
                                 state = rememberSharedContentState(key = "playlist_title_${playlist.id}"),
                                 animatedVisibilityScope = animatedVisibilityScope
                             )
                         }
                     } else Modifier
                )
        )
        
        Text(
            text = if (coverSong != null) "主打: ${coverSong.title}" else "${playlist.songs.size} 首歌",
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 1
        )
    }
}
