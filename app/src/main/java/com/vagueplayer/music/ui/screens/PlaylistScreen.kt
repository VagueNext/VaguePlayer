package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.vagueplayer.music.ui.components.bouncyClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vagueplayer.music.viewmodel.AudioViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.vagueplayer.music.ui.theme.AccentBlue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow

/**
 * Simplified PlaylistScreen - No SharedElement Animations
 * 干净简洁的歌单页面，专注于正确的显示和交互
 */
@Composable
fun PlaylistScreen(
    onCreatePlaylist: () -> Unit,
    onShowAddMenu: (Offset) -> Unit,
    onOverlayBounds: (androidx.compose.ui.geometry.Rect) -> Unit,
    hazeState: HazeState? = null,
    onPlaylistClick: (Playlist) -> Unit,
    onContextMenuRequest: (Playlist, Offset) -> Unit, // New callback
    animatedVisibilityScope: Any? = null,
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> }
) {
    val effectiveHazeState = hazeState ?: remember { HazeState() }
    
    val context = LocalContext.current
    val viewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(context))
    val playlists by viewModel.userPlaylists.collectAsState()

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    
    val scrollAlpha = remember {
        derivedStateOf {
            val firstVisibleItemIndex = gridState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
            
            val threshold = 50f
            val scrollY = firstVisibleItemIndex * 300f + firstVisibleItemScrollOffset
            
            (scrollY / threshold).coerceIn(0f, 1f)
        }
    }.value

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .haze(effectiveHazeState)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(playlists) { playlist ->
                // Track position for menu
                var cardPosition by remember { mutableStateOf(Offset.Zero) }
                
                Box(modifier = Modifier.onGloballyPositioned { cardPosition = it.boundsInWindow().center }) {
                    SimplePlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist) },
                        onLongClick = { onContextMenuRequest(playlist, cardPosition) }
                    )
                }
            }
        }

        com.vagueplayer.music.ui.components.ScreenHeader(
            title = "歌单",
            scrollAlpha = scrollAlpha,
            hazeState = effectiveHazeState,
            modifier = Modifier.align(Alignment.TopCenter),
            action = {
                Box {
                    var localBtnPos by remember { mutableStateOf(Offset.Zero) }
                    
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
    }
}

/**
 * Simplified Playlist Card - No Animations
 * 简单的歌单卡片，确保封面完整显示
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SimplePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(
                targetScale = 0.95f, 
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 封面图片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.songs.firstOrNull()?.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 歌单信息
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = playlist.name,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.songs.size} 首歌曲",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
