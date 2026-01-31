package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.vagueplayer.music.ui.components.bouncyClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot
import com.vagueplayer.music.ui.theme.AccentBlue
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.spring

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistScreen(
    onCreatePlaylist: () -> Unit,
    onShowAddMenu: (androidx.compose.ui.geometry.Offset) -> Unit,
    onOverlayBounds: (androidx.compose.ui.geometry.Rect) -> Unit,
    hazeState: HazeState? = null,
    onPlaylistClick: (Playlist) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> }
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

    val isSidebarLeft by viewModel.isSidebarOnLeft.collectAsState()

    val coroutineScope = rememberCoroutineScope()

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
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onLongClick = { onShowAddMenu(Offset.Zero) },
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }

        com.vagueplayer.music.ui.components.ScreenHeader(
            title = "歌单",
            scrollAlpha = scrollAlpha,
            hazeState = effectiveHazeState,
            modifier = Modifier
                .align(Alignment.TopCenter),
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
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(
                targetScale = 0.95f, 
                onClick = onClick,
                onLongClick = onLongClick
             )
    ) {
        // Shared Container Background (Decoupled)
        Box(
            modifier = Modifier
                .matchParentSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "container_${playlist.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) },
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        )

        // Content Layer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)) // Clip content to match card shape
                // No background here, using shared background
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .sharedElement(
                        state = rememberSharedContentState(key = "cover_${playlist.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) }
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder background for image
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.songs.firstOrNull()?.albumArtUri)
                        .crossfade(false)
                        .memoryCacheKey("cover_${playlist.id}")
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Text Info
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = playlist.name,
                    fontSize = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.songs.size} 首歌曲",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
