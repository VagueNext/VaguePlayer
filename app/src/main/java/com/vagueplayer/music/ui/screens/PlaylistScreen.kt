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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow

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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    onCreatePlaylist: () -> Unit,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val viewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(context))
    val playlists by viewModel.userPlaylists.collectAsState()
    val allSongs by viewModel.songs.collectAsState()

    // Dialog & UI States
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    
    var showMenu by remember { mutableStateOf(false) }
    var isExportMode by remember { mutableStateOf(false) }
    var playlistToExportId by remember { mutableStateOf<String?>(null) } // Changed to String



    // Management States
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // File Pickers
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
            isExportMode = false // Reset mode
            playlistToExportId = null
        }
    )

    // Root Box for Z-Layering (Content + Overlays)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { 
                    // Click outside to dismiss menu
                    if (showMenu) showMenu = false 
                }
        ) {
            Column {
                // Header
                com.vagueplayer.music.ui.components.ScreenHeader(
                    title = if (isExportMode) "选择导出歌单" else "歌单",
                    action = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // Action Menu Overlay - Using Morphing Glass Menu
                            if (showMenu) {
                                PlaylistActionMenu(
                                    isExpanded = true,
                                    anchorSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
                                    onAddPlaylist = {
                                        showMenu = false
                                        onCreatePlaylist()
                                    },
                                    onImportPlaylist = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("text/plain"))
                                    },
                                    onExportPlaylist = {
                                        showMenu = false
                                        isExportMode = true
                                        Toast.makeText(context, "请点击下方歌单进行导出", Toast.LENGTH_SHORT).show()
                                    },
                                    onDismiss = { showMenu = false },
                                    hazeState = hazeState
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
                                    selectedPlaylist = playlist
                                    showDetailDialog = true
                                }
                            },
                            onLongClick = {
                                if (!isExportMode) {
                                    selectedPlaylist = playlist
                                    showDeleteDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    

    
    // Delete Dialog (Unified Interface)
    if (showDeleteDialog && selectedPlaylist != null) {
        com.vagueplayer.music.ui.components.GlassAlertDialog(
            hazeState = hazeState,
            title = "删除歌单",
            description = "确定要删除歌单 '${selectedPlaylist!!.name}' 吗？此操作无法撤销。",
            icon = Icons.Default.Delete,
            confirmText = "删除",
            onConfirm = {
                selectedPlaylist?.let { viewModel.deletePlaylist(it.id) }
                showDeleteDialog = false
                selectedPlaylist = null
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Rename Dialog (Unified Interface)
    if (showRenameDialog && selectedPlaylist != null) {
        // Pre-fill logic is handled by initialValue, but we need to ensure state is fresh
        // Since InputDialog uses 'remember { initialValue }', it might not update if key isn't changed.
        // But here showRenameDialog toggles, so the composable enters/leaves composition.
        
        com.vagueplayer.music.ui.components.GlassInputDialog(
            hazeState = hazeState,
            title = "重命名歌单",
            initialValue = renameText, // Passed from state
            icon = Icons.Default.Edit,
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                     selectedPlaylist?.let { viewModel.renamePlaylist(it.id, newName) }
                }
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
    
    // Create Playlist Dialog logic hoisted to MainScreen...

    // Playlist Detail & Add Song Logic
    if (showDetailDialog && selectedPlaylist != null) {
        val playlist = selectedPlaylist!!
        // Re-read playlist from list to get updates
        val currentPlaylist = playlists.find { it.id == playlist.id } ?: playlist

        Dialog(onDismissRequest = { 
            showDetailDialog = false 
            selectedPlaylist = null
        }) {
            // Glass Detail Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .clip(RoundedCornerShape(24.dp))
                    // .waterDropGlass removed from parent
            ) {
                // 1. Background (Glass)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .waterDropGlass(
                            hazeState = hazeState,
                            cornerRadius = 24.dp
                        )
                )

                // 2. Content
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Text(
                        text = currentPlaylist.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.clickable {
                             renameText = currentPlaylist.name
                             showRenameDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (currentPlaylist.songs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Text("暂无歌曲", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentPlaylist.songs) { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playSong(song, currentPlaylist.songs, currentPlaylist.name) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Album Art with Shadow
                                    AsyncImage(
                                        model = song.albumArtUri,
                                        contentDescription = "Cover",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray),
                                        contentScale = ContentScale.Crop,
                                        error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_crop)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Text Info
                                    Column {
                                        Text(
                                            text = song.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black.copy(alpha = 0.9f),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 12.sp,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            maxLines = 1
                                        )
                                    }
                                }
                                // No divider needed for clean look
                            }
                        }
                    }
                }
            }
            } // End Root Box
}
        
        // Add Song Picker (Nested)

    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    coverSong: Song?, // Passed from outside
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.5f)), // Fallback glass
            contentAlignment = Alignment.Center
        ) {
            if (coverSong != null) {
                // Show Most Played Song Cover
                AsyncImage(
                    model = coverSong.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Optional: Scrim to make text readable if we put text over it?
                // The design shows text BELOW the card.
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
        
        Text(
            text = playlist.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black.copy(alpha = 0.8f),
            maxLines = 1
        )
        
        Text(
            text = if (coverSong != null) "主打: ${coverSong.title}" else "${playlist.songs.size} 首歌",
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 1
        )
    }
}
