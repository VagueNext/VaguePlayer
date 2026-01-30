package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background // [FIX]
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable // [FIX] Import
import com.vagueplayer.music.ui.components.bouncyClickable // [FIX] Added import
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
import androidx.compose.ui.unit.sp // [FIX]
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
import androidx.compose.material.icons.filled.Delete
import com.vagueplayer.music.ui.components.bouncyClickable // [FIX] Added import
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot // [FIX] Added import
import com.vagueplayer.music.ui.theme.AccentBlue

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistScreen(
    onCreatePlaylist: () -> Unit,
    onShowAddMenu: (androidx.compose.ui.geometry.Offset) -> Unit,
    onOverlayBounds: (androidx.compose.ui.geometry.Rect) -> Unit, // [NEW] Unified Lens Support
    hazeState: HazeState? = null,
    onPlaylistClick: (Playlist) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> } // [NEW] Add this
) {
    // [FIX] Use effective state (from MainScreen or local fallback)
    val effectiveHazeState = hazeState ?: remember { HazeState() }
    
    val context = LocalContext.current
    val viewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(context))
    val playlists by viewModel.userPlaylists.collectAsState()
    
    // Management States
    // isManageMode removed - using long press for delete instead
    // Management States
    // isManageMode removed - using long press for delete instead

    // Scroll State
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    
    // Calculate Header Alpha based on scroll
    val scrollAlpha = remember {
        derivedStateOf {
            val firstVisibleItemIndex = gridState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset
            
            // Start fading in after 50dp scroll
            val threshold = 50f
            val scrollY = firstVisibleItemIndex * 300f + firstVisibleItemScrollOffset // Approximate
            
            (scrollY / threshold).coerceIn(0f, 1f)
        }
    }.value

    // Sidebar position setting
    val isSidebarLeft by viewModel.isSidebarOnLeft.collectAsState()

    // Coroutine scope for scrolling
    val coroutineScope = rememberCoroutineScope()

    // Root Container
    // [NEW] Haze State for background blur

    Box(modifier = Modifier.fillMaxSize()) {
        // Playlist Grid (Below Header)
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .haze(effectiveHazeState), // [FIX] Mark as Source
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                top = 100.dp, // Space for floating header
                bottom = 120.dp,
                start = 20.dp,
                end = 20.dp
            ),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { 
                        onPlaylistClick(playlist) 
                    },
                    onLongClick = {
                         viewModel.requestDeletePlaylist(playlist)
                    },
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }

        // Floating Header (On Top)
        com.vagueplayer.music.ui.components.ScreenHeader(
            title = "歌单",
            scrollAlpha = scrollAlpha,
            hazeState = effectiveHazeState, // [FIX] Use effective state
            modifier = Modifier
                .align(Alignment.TopCenter),
            action = {
                Box {
                    var localBtnPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    
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

        
        // [FIX] Delete Dialog Logic Moved to MainScreen (Hoisted)
        // When Delete invoked via Menu, just call ViewModel
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
            .bouncyClickable(
                targetScale = 0.95f, 
                onClick = onClick,
                onLongClick = onLongClick
             )
            // [FIX] Shared Bounds on Wrapper (Container Transform)
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "container_${playlist.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White) // Card Background
    ) {
        // 1. Image Area
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playlist.songs.firstOrNull()?.albumArtUri)
                    .crossfade(true)
                    .memoryCacheKey("cover_${playlist.id}")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Text Info (Outside Shared Bounds for cleaner text fade)
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = playlist.name,
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier // Removed nested sharedBounds
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
