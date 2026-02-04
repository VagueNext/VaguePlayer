package com.vagueplayer.music.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.vagueplayer.music.data.model.Playlist
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.unit.Velocity

import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vagueplayer.music.ui.animation.ExpandableContainer
import com.vagueplayer.music.ui.animation.transformSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.List

import androidx.compose.material3.IconButton
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vagueplayer.music.ui.components.NavItems
import com.vagueplayer.music.ui.components.MiniPlayer
import com.vagueplayer.music.ui.components.SwipeablePager

import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow


import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search


import androidx.compose.foundation.lazy.items



import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

import com.vagueplayer.music.ui.screens.SettingsScreen
import com.vagueplayer.music.ui.screens.FolderManagerScreen
import com.vagueplayer.music.ui.components.liquidGlassLens


@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    // 1. Permission Check
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission)

    if (!permissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("需要访问存储权限以播放音乐", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("授予权限")
    
                }
            }
        }
        return
    }

    // 2. Main Content
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember { com.vagueplayer.music.viewmodel.AudioViewModelFactory(context) }
    val audioViewModel: com.vagueplayer.music.viewmodel.AudioViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

    // Top-Level State
    var currentPage by remember { mutableIntStateOf(0) }
    var playerBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var navBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var searchBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var overlayBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    // State
    var showPlayer by remember { mutableStateOf(false) }
    var showPlaylistGlobal by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<com.vagueplayer.music.data.model.Playlist?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var activeSongForMenu by remember { mutableStateOf<com.vagueplayer.music.data.model.Song?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var songMenuAnchor by remember { mutableStateOf(Offset.Zero) }
    var songMenuSize by remember { mutableStateOf(androidx.compose.ui.unit.DpSize(48.dp, 48.dp)) }
    
    val importPlaylistLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { audioViewModel.importPlaylistFromTxt(it) }
        }
    )
    
    // Quick Action States
    var showFavoritesOverlay by remember { mutableStateOf(false) }
    var showRecentOverlay by remember { mutableStateOf(false) }
    var showRemovedOverlay by remember { mutableStateOf(false) }

    // Hoisted Sort Menu State
    var showSortMenu by remember { mutableStateOf(false) }
    var sortMenuAnchor by remember { mutableStateOf(Offset.Zero) }

    // Hoisted Playlist Action Menu State
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var playlistMenuAnchor by remember { mutableStateOf(Offset.Zero) }

    // Hoisted Repeat Menu State
    var showRepeatMenu by remember { mutableStateOf(false) }
    var repeatMenuAnchor by remember { mutableStateOf(Offset.Zero) }
    // Loop Count Dialog State
    var showLoopCountDialog by remember { mutableStateOf(false) }
    var tempLoopCount by remember { mutableStateOf("") }

    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // Haze State
    val mainHazeState = remember { HazeState() }
    
    // Scroll Animation State
    // 0f = Expanded (Normal), 1f = Collapsed (Circle/Shrunk)
    // Controlled by Swipe Threshold
    var isDockCollapsed by remember { mutableStateOf(false) } 
    // Accumulator for scroll delta to detect swipe intent
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 5.dp
    val density = LocalDensity.current
    
    // Use Animatable for velocity support
    val dockCollapseAnimatable = remember { Animatable(0f) }
    // Store velocity from fling to pass to Animatable
    var flingVelocityY by remember { mutableFloatStateOf(0f) }

    // [ANIMATION] Drive the Animatable with Physics
    // We use a separate trigger for flings because the boolean state 'isDockCollapsed' might have already 
    // been toggled by the drag threshold (onPreScroll), so 'LaunchedEffect(isDockCollapsed)' won't re-run 
    // when 'onPreFling' happens later with the actual velocity.
    var flingTrigger by remember { mutableLongStateOf(0L) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // ... (Existing Scroll Logic Unchanged) ...
                val delta = available.y
                scrollAccumulator += delta
                val thresholdPx = with(density) { swipeThreshold.toPx() }
                
                // Direction-Aware Accumulation
                if (!isDockCollapsed) {
                    if (delta < 0) {
                        scrollAccumulator += delta
                        if (scrollAccumulator < -thresholdPx) {
                            isDockCollapsed = true
                            scrollAccumulator = 0f
                        }
                    } else { scrollAccumulator = 0f }
                } else {
                    if (delta > 0) {
                        scrollAccumulator += delta
                        if (scrollAccumulator > thresholdPx) {
                            isDockCollapsed = false
                            scrollAccumulator = 0f
                        }
                    } else { scrollAccumulator = 0f }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                 // REVERTED: User requested removal of velocity logic.
                 // We simply return Zero to ensure the list consumes the fling, 
                 // but we do NOT hijack the animation rate anymore.
                 return Velocity.Zero
            }
        }
    }
    
    // [ANIMATION] Standard Dock Animation (No Velocity Injection)
    LaunchedEffect(isDockCollapsed) {
        val target = if (isDockCollapsed) 1f else 0f
        
        // Standard Spring for consistent feel
        val targetSpec = spring<Float>(
            dampingRatio = 0.75f,
            stiffness = 1000f // Balanced stiffness
        )

        dockCollapseAnimatable.animateTo(
            targetValue = target,
            animationSpec = targetSpec
        )
    }


    // Unified Overlay Check for Global UI logic (Back Button, Focus, etc.)
    val isAnyOverlayVisible = showSettings || showPlaylistMenu || showRepeatMenu || showSortMenu || 
                              showPlaylistGlobal || showDeleteConfirm || showCreatePlaylistDialog || 
                              showAddToPlaylist || showFavoritesOverlay || showRecentOverlay || 
                              showRemovedOverlay || showLoopCountDialog

    // Force Dock Expansion in Sub-screens
    // When Settings or Playlist Detail is open, we want the player to be prominent (Expanded).
    val effectiveCollapseProgress = if (isAnyOverlayVisible) 0f else dockCollapseAnimatable.value
    
    // Consolidate Dock Visibility
    // The dock should only hide if the full Player is visible.
    // It should stay visible in Search (Mini Mode), Settings, and Playlists.
    val isDockVisible = !showPlayer
    val dockCollapseProgress = dockCollapseAnimatable.value
    
    // Glass Config (Now using LiquidGlassDefaults globally) 

    // Selection State
    val isSelectionMode by audioViewModel.isSelectionMode.collectAsState()
    val selectedIds by audioViewModel.selectedIds.collectAsState()
    val songs by audioViewModel.songs.collectAsState()
    
    val favoriteIds by audioViewModel.favoriteIds.collectAsState()
    val playCounts by audioViewModel.playCounts.collectAsState()
    

    // Search State
    var searchText by remember { mutableStateOf("") }
    // val searchResults REMOVED (Replaced by searchUiState)
    
    LaunchedEffect(searchText) {
        audioViewModel.updateSearchQuery(searchText)
    }


    @OptIn(ExperimentalSharedTransitionApi::class)
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection) 
        ) {
        
         // Animate distortion strength: Fade out when Player is open to prevent full-screen glitch
        val targetDistortion = if (showPlayer) 0f else 45f
        val animatedDistortion by animateFloatAsState(
            targetValue = targetDistortion,
            animationSpec = tween(300), 
            label = "glassDistortion"
        )

        // WRAPPER BOX for Content
        Box(
             modifier = Modifier
                .fillMaxSize()
                .liquidGlassLens(
                    bounds1 = playerBounds,
                    bounds2 = navBounds,
                    bounds3 = searchBounds,
                    bounds4 = overlayBounds,
                    distortionStrength = animatedDistortion, 
                    edgeWidth = 60f,
                    fusionStrength = 35f,
                    aberrationStrength = 0.3f,
                    tint = Color.White.copy(alpha = 0.80f),
                    enableShader = true
                )
        ) {
        
        // -------------------------------------------------------------------------
        // SOURCE LAYER: Content to be blurred
        // -------------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Inner Box for Haze Source Capture (Raw Content)
            Box(
                 modifier = Modifier
                    .fillMaxSize()
                    // .haze(state = mainHazeState) // REMOVED: Pushed down to children to avoid Sink-in-Source crash
            ) {
            // Background (Pure White)\n            Box(modifier = Modifier.fillMaxSize().background(Color.White))

            // Wrapper for Search/Pager content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // REMOVED padding(bottom = 100.dp) to allow content to scroll BEHIND the dock
            ) {
                // Search Mode UI
                if (isSearchActive) {
                    val uiState by audioViewModel.searchUiState.collectAsState()
                    val songs by audioViewModel.songs.collectAsState() // Ensure we have the full list if search is empty
                    
                    // Logic: If search invalid/empty, show ALL songs (Library Mode in Search UI?)
                    // Actually, usually search UI shows nothing or "History" if empty.
                    // But original code showed 'songs' if searchText was empty.
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            // haze modifier removed from here
                            .padding(horizontal = 16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 20.dp, 
                            bottom = 120.dp 
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. EMPTY QUERY / NO RESULTS CASE
                        if (searchText.isEmpty()) {
                             // Show Full Library (Legacy Behavior)
                             items(songs) { song ->
                                 SongItem(song, selectedIds, isSelectionMode, audioViewModel)
                             }
                        } else if (uiState.meta.isEmpty() && uiState.lyrics.isEmpty() && !uiState.isSearchingLyrics) {
                             item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                    Text("未找到相关歌曲", color = Color.Gray)
                                }
                            }
                        } else {
                            // 2. META RESULTS (Track 1)
                            if (uiState.meta.isNotEmpty()) {
                                item {
                                    Text("最佳匹配", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                                }
                                items(uiState.meta) { song ->
                                    SongItem(song, selectedIds, isSelectionMode, audioViewModel, matchType = "Meta")
                                }
                            }
                            
                            // 3. GLASS DIVIDER
                            if (uiState.meta.isNotEmpty() && (uiState.lyrics.isNotEmpty() || uiState.isSearchingLyrics)) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                            .height(1.dp)
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.1f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                            
                            // 4. LYRICS RESULTS (Track 2)
                            if (uiState.lyrics.isNotEmpty()) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("歌词匹配", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                                        if (uiState.isSearchingLyrics) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                                items(uiState.lyrics) { song ->
                                    SongItem(song, selectedIds, isSelectionMode, audioViewModel, matchType = "Lyrics")
                                }
                            } else if (uiState.isSearchingLyrics) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Gray.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
            } // End Search Mode block

            // Pager Layer (Background) - Visible when Settings is NOT open
            // Note: If Search is active, Pager is hidden by Search UI (z-order or logic).
            // But to be safe and replicate original else logic:
            if (!isSearchActive) {
                AnimatedVisibility(
                    visible = !showSettings,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    SwipeablePager(
                        pageCount = 3,
                        selectedIndex = currentPage,
                        onPageChanged = { currentPage = it }
                    ) { pageIndex ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (pageIndex) {
                                0 -> LibraryScreen(
                                    hazeState = mainHazeState,
                                    onShowSortOptions = { anchor ->
                                        sortMenuAnchor = anchor
                                        showSortMenu = true
                                    },
                                    onSongMenuRequest = { song, offset, size ->
                                        activeSongForMenu = song
                                        songMenuAnchor = offset
                                        size?.let { songMenuSize = it } ?: run { songMenuSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp) }
                                        showSongMenu = true
                                    }
                                )
                                    // [Refactor] Wrap in AnimatedVisibility for Shared Element Scope & Fade Out
                                    1 -> { 
                                         // Keep Grid visible underneath Detail to ensure Source is available for return transition
                                         val innerScope = this
                                         // Standard Pager doesn't offer AnimatedVisibilityScope.
                                         // But PlaylistScreen EXPECTS one.
                                         // We'll wrap it in a pseudo-scope or just force it visible.
                                         // To satisfy the @Composable signature requiring 'animatedVisibilityScope':
                                         // We can use an infinite AnimatedVisibility.
                                         androidx.compose.animation.AnimatedVisibility(
                                             visible = true, // Always visible
                                             enter = androidx.compose.animation.EnterTransition.None,
                                             exit = androidx.compose.animation.ExitTransition.None,
                                             modifier = Modifier.fillMaxSize() 
                                         ) {
                                            val avScope = this
                                            with(this@SharedTransitionLayout) {
                                                PlaylistScreen(
                                                    onCreatePlaylist = { showCreatePlaylistDialog = true },
                                                    onShowAddMenu = { anchor ->
                                                        playlistMenuAnchor = anchor
                                                        showPlaylistMenu = true
                                                    },
                                                    onOverlayBounds = { 
                                                        overlayBounds = if (it.width <= 0f || it.height <= 0f) null else it 
                                                    },
                                                    onPlaylistClick = { playlist -> 
                                                        selectedPlaylist = playlist 
                                                    },
                                                    hazeState = mainHazeState,
                                                    animatedVisibilityScope = avScope,
                                                    onSongMenuRequest = { song, offset, size ->
                                                        activeSongForMenu = song
                                                        songMenuAnchor = offset
                                                        size?.let { songMenuSize = it } ?: run { songMenuSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp) }
                                                        showSongMenu = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                2 -> ProfileScreen(
                                    // hazeState = null, (Removed)
                                    onNavigateToSettings = { showSettings = true },
                                    onQuickAction = { action ->
                                        when (action) {
                                            "recent" -> showRecentOverlay = true
                                            "favorites" -> showFavoritesOverlay = true
                                            "removed" -> showRemovedOverlay = true
                                        }
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                    hazeState = mainHazeState
                                )
                            }
                        }
                    }
                }
            }
        } // End Search/Pager Wrapper
        // -------------------------------------------------------------------------
        // SINK LAYER: Glass Overlays & Dialogs (Must be Siblings of Source)
        // -------------------------------------------------------------------------


        } // End Inner Haze Box
        } // End Source Layer Box

        // Dock removed from here

                               
                               


        // 2. Search Floating Pill (Sibling)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(10f)
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 5.dp)
                .padding(horizontal = 16.dp)
        ) {
             LaunchedEffect(Unit) {
                 searchFocusRequester.requestFocus()
             }
             
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(19.dp), spotColor = Color(0x20000000))
                    .clip(RoundedCornerShape(19.dp))
            ) {
                     // 1. Background Layer (Glass)
                 Box(
                     modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.85f))
                 )

                 // 2. Content Layer
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.fillMaxSize()
                 ) {
                     Box(
                         modifier = Modifier.size(38.dp), // Match height
                         contentAlignment = Alignment.Center
                     ) {
                         androidx.compose.material3.Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                     }
                     
                     androidx.compose.foundation.text.BasicTextField(
                         value = searchText,
                         onValueChange = { searchText = it },
                         modifier = Modifier
                             .weight(1f)
                             .focusRequester(searchFocusRequester),
                         textStyle = androidx.compose.ui.text.TextStyle(
                             color = Color.Black.copy(alpha = 0.8f),
                             fontSize = 15.sp
                         ),
                         singleLine = true,
                         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                         keyboardActions = KeyboardActions(onSearch = { 
                             // Close keyboard logic if needed
                         }),
                         decorationBox = { innerTextField ->
                             if (searchText.isEmpty()) {
                                 Text("搜索音乐...", color = Color.Gray.copy(alpha = 0.5f), fontSize = 18.sp)
                             }
                             innerTextField()
                         }
                     )
                     if (searchText.isNotEmpty()) {
                         androidx.compose.material3.IconButton(onClick = { searchText = "" }) {
                             androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Delete, null, tint = Color.Gray)
                         }
                     }
                 }
            }
        }

        // =========================================================================
        // DIALOG LAYER: Outside Haze Box to avoid descendant crash
        // hazeChild in dialogs will sample from the haze source above
        // =========================================================================

        // Dialogs NOW OUTSIDE Haze Box (as siblings, not descendants)
        if (showCreatePlaylistDialog) {
            var newName by remember { mutableStateOf("") }
            com.vagueplayer.music.ui.components.GlassDialog(
                enableShader = true,
                hazeState = mainHazeState,
                onDismissRequest = { 
                    showCreatePlaylistDialog = false
                    overlayBounds = null
                },
                title = "新建歌单",
                description = "请输入歌单名称",
                confirmText = "创建",
                onConfirm = {
                    if (newName.isNotBlank()) {
                        audioViewModel.createUserPlaylist(newName)
                        showCreatePlaylistDialog = false
                        overlayBounds = null
                    }
                },
                cancelText = "取消",
                onLayoutCoordinates = { overlayBounds = it.boundsInRoot() },
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (newName.isEmpty()) {
                            Text(text = "歌单名称", color = Color.Gray, fontSize = 16.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
        
        if (showDeleteConfirm) {
            com.vagueplayer.music.ui.components.GlassDialog(
                // Defaults applied automatically
                enableShader = true,
                hazeState = mainHazeState,
                onDismissRequest = { 
                    showDeleteConfirm = false
                    overlayBounds = null
                },
                title = "确认移除?",
                description = "选中的 ${selectedIds.size} 首歌曲将从列表中移除，但不会删除本地文件。",
                confirmText = "移除",
                onConfirm = {
                    val toDelete = songs.filter { it.id in selectedIds }
                    audioViewModel.deleteSongs(toDelete)
                    showDeleteConfirm = false
                    audioViewModel.clearSelection() 
                    overlayBounds = null
                },
                cancelText = "取消",
                onLayoutCoordinates = { overlayBounds = it.boundsInRoot() }
            )
        }



        // Shared Element Container Transition ("Hero Animation")
        // Split AnimatedVisibility:
        // 1. Dimming Layer: Fades out immediately (Standard)
        // 2. Player Container: Stays Opaque during Morph (Delayed Fade) to ensure "Shrink" is visible.
        
        // Unified Container Transform (Player Expand)
        ExpandableContainer(
            isExpanded = showPlayer,
            key = "container_transform",
            onDismissRequest = { 
                showPlayer = false 
                showRepeatMenu = false
                showSortMenu = false
                showPlaylistMenu = false
                overlayBounds = null
            },
            modifier = Modifier.fillMaxSize(), // Target expands to Full Screen
            // sharedTransitionScope = this@SharedTransitionLayout, // Implicit receiver
        ) {
            PlayerScreen(
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@ExpandableContainer, // Scope from ExpandableContainer's internal AnimatedVisibility
                viewModel = audioViewModel,
                onDismiss = { showPlayer = false },
                onTogglePlaylist = { showPlaylistGlobal = true },
                onShowRepeatMenu = { anchor ->
                    repeatMenuAnchor = anchor
                    showRepeatMenu = true
                },
                isOverlayVisible = isAnyOverlayVisible
            )
        }



        // Playlist Overlays - Z-Index: Top of Player (100f)
        Box(modifier = Modifier.zIndex(100f).fillMaxSize()) {
            com.vagueplayer.music.ui.components.GlassPlaylistOverlay(
                viewModel = audioViewModel,
                isVisible = showPlaylistGlobal,
                onDismiss = { showPlaylistGlobal = false }
            )

            if (showAddToPlaylist) {
                com.vagueplayer.music.ui.components.GlassPlaylistOverlay(
                    viewModel = audioViewModel,
                    isVisible = true,
                    onDismiss = { showAddToPlaylist = false },
                    addToPlaylistMode = true,
                    songsToAdd = songs.filter { it.id in selectedIds }
                )
            }

            // Favorites Overlay
            if (showFavoritesOverlay) {
                 val favSongs = songs.filter { it.id in favoriteIds }
                 com.vagueplayer.music.ui.components.GlassPlaylistOverlay(
                    viewModel = audioViewModel,
                    isVisible = true,
                    onDismiss = { showFavoritesOverlay = false },
                    customListMode = true,
                    customSongs = favSongs,
                    customTitle = "收藏歌曲"
                )
            }
            
            // Recent Overlay
            if (showRecentOverlay) {
                 val recentSongs = songs
                     .filter { (playCounts[it.id] ?: 0) > 0 }
                     .sortedByDescending { playCounts[it.id] ?: 0 }
                 
                 com.vagueplayer.music.ui.components.GlassPlaylistOverlay(
                    viewModel = audioViewModel,
                    isVisible = true,
                    onDismiss = { showRecentOverlay = false },
                    customListMode = true,
                    customSongs = recentSongs,
                    customTitle = "最近播放"
                )
            }
            
            // Removed (Hidden) Songs Overlay
            if (showRemovedOverlay) {
                 val hiddenSongs by audioViewModel.hiddenSongs.collectAsState()
                 com.vagueplayer.music.ui.components.GlassPlaylistOverlay(
                    viewModel = audioViewModel,
                    isVisible = true,
                    onDismiss = { showRemovedOverlay = false },
                    customListMode = true,
                    customSongs = hiddenSongs,
                    customTitle = "已移除歌曲"
                )
            }
        }



        // Playlist Detail Container Transform
        // BackHandler moved to PlaylistDetailScreen for better predictive back support

        // Playlist Detail Container Transform
        // Replaces old AnimatedContent to allow Shared Element Scope to pass through
        val currentPlaylist = selectedPlaylist
        androidx.compose.animation.AnimatedVisibility(
            visible = currentPlaylist != null,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)),
            modifier = Modifier.zIndex(100f).fillMaxSize()
        ) { 
            if (currentPlaylist != null) {
                // Determine cover URL
                val coverUrl = audioViewModel.getMostPlayedSong(currentPlaylist)?.albumArtUri?.toString() 
                    ?: currentPlaylist.songs.firstOrNull()?.albumArtUri?.toString()
                
                PlaylistDetailScreen(
                    playlist = currentPlaylist,
                    viewModel = audioViewModel,
                    onDismissRequest = { selectedPlaylist = null },
                    animatedVisibilityScope = this, // Now refers to AnimatedVisibilityScope from AnimatedVisibility
                    hazeState = mainHazeState,
                    onSongMenuRequest = { song, offset, size ->
                        activeSongForMenu = song
                        songMenuAnchor = offset
                        size?.let { songMenuSize = it } ?: run { songMenuSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp) }
                        showSongMenu = true
                    }
                )
            }
        }



        




        // Hoisted Sort Menu Overlay
        com.vagueplayer.music.ui.components.MorphingGlassMenu(
            isExpanded = showSortMenu,
                onDismiss = { 
                    showSortMenu = false
                    overlayBounds = null
                },
                anchorSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
                anchorPosition = sortMenuAnchor, 
                onLayoutCoordinates = { overlayBounds = it.boundsInRoot() }
            ) {
                  val currentSort = audioViewModel.sortOption.collectAsState().value
                  val options: List<Pair<String, com.vagueplayer.music.viewmodel.AudioViewModel.SortOption>> = listOf(
                      "标题" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.TITLE,
                      "艺术家" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.ARTIST,
                      "大小" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.SIZE,
                      "播放次数" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.PLAY_COUNT,
                      "时长 (短→长)" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.DURATION_ASC,
                      "时长 (长→短)" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.DURATION_DESC,
                      "添加时间" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.DATE_ADDED
                  )
                  
                  options.forEach { (label, option) ->
                      Row(
                           modifier = Modifier
                               .fillMaxWidth()
                               .clickable { 
                                   audioViewModel.setSortOption(option)
                                   showSortMenu = false
                                   overlayBounds = null
                               }
                               .padding(vertical = 8.dp, horizontal = 20.dp), 
                           horizontalArrangement = Arrangement.SpaceBetween,
                           verticalAlignment = Alignment.CenterVertically
                      ) {
                          Text(
                              text = label, 
                              color = if(currentSort == option) com.vagueplayer.music.ui.theme.AccentBlue else Color.Black, 
                              fontSize = 15.sp, 
                              fontWeight = FontWeight.Medium
                          )
                          
                          Box(
                              modifier = Modifier
                                  .size(20.dp)
                                  .border(
                                      width = 2.dp, 
                                      color = if (currentSort == option) com.vagueplayer.music.ui.theme.AccentBlue else Color.Gray.copy(alpha = 0.5f), 
                                      shape = androidx.compose.foundation.shape.CircleShape
                              ),
                              contentAlignment = Alignment.Center
                          ) {
                              if (currentSort == option) {
                                  Box(
                                      modifier = Modifier
                                          .size(10.dp)
                                          .background(com.vagueplayer.music.ui.theme.AccentBlue, androidx.compose.foundation.shape.CircleShape)
                                  )
                              }
                          }
                      }
                  }
            }



        // Hoisted Playlist Action Menu
        com.vagueplayer.music.ui.components.PlaylistActionMenu(
            isExpanded = showPlaylistMenu,
                anchorSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
                anchorPosition = playlistMenuAnchor,
                hazeState = mainHazeState,
                onLayoutCoordinates = { overlayBounds = it.boundsInRoot() },
                onAddPlaylist = {
                    showPlaylistMenu = false
                    showCreatePlaylistDialog = true
                    // Note: showCreatePlaylistDialog will take over overlayBounds
                },
                onImportPlaylist = {
                    showPlaylistMenu = false
                    overlayBounds = null
                    importPlaylistLauncher.launch(arrayOf("text/plain"))
                },
                onExportPlaylist = {
                    showPlaylistMenu = false
                    overlayBounds = null
                },
                onDismiss = { 
                    showPlaylistMenu = false
                    overlayBounds = null
                },
        )

        // Selection Action Menu
        val selectedCount = selectedIds.size
        com.vagueplayer.music.ui.components.MorphingGlassMenu(
            isExpanded = showSelectionMenu,
            onDismiss = { showSelectionMenu = false },
            anchorSize = androidx.compose.ui.unit.DpSize(0.dp, 0.dp), // Use minimal anchor
            anchorPosition = playerBounds?.let { 
                 Offset(it.right - with(density) { 40.dp.toPx() }, it.top + with(density) { 20.dp.toPx() }) 
            } ?: Offset.Zero,
            hazeState = mainHazeState,
            onLayoutCoordinates = { overlayBounds = it.boundsInRoot() }
        ) {
              Column(modifier = Modifier.width(160.dp).padding(vertical = 8.dp)) {
                  // Title
                  Text(
                      text = "已选 $selectedCount 首",
                      fontSize = 13.sp,
                      color = Color.Gray,
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                  )
                  
                  // 1. Play
                  Row(
                      modifier = Modifier
                          .fillMaxWidth()
                          .clickable { 
                               val selectedSongs = songs.filter { it.id in selectedIds }
                               if (selectedSongs.isNotEmpty()) {
                                   audioViewModel.playSong(selectedSongs.first(), selectedSongs, "已选歌曲")
                                   audioViewModel.clearSelection()
                               }
                               showSelectionMenu = false
                          }
                          .padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      androidx.compose.material3.Icon(
                          imageVector = Icons.Default.PlayArrow, 
                          contentDescription = "播放", 
                          tint = Color.Black.copy(alpha = 0.8f),
                          modifier = Modifier.size(20.dp)
                      )
                      Spacer(Modifier.width(12.dp))
                      Text("播放", fontSize = 15.sp, color = Color.Black)
                  }
                  
                  // 2. Play Next
                  Row(
                      modifier = Modifier
                          .fillMaxWidth()
                          .clickable { 
                               val selectedSongs = songs.filter { it.id in selectedIds }
                               if (selectedSongs.isNotEmpty()) {
                                   audioViewModel.addToNext(selectedSongs)
                                   audioViewModel.clearSelection()
                               }
                               showSelectionMenu = false
                          }
                          .padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      androidx.compose.material3.Icon(
                          imageVector = Icons.AutoMirrored.Filled.PlaylistPlay, 
                          contentDescription = "下一首播放", 
                          tint = Color.Black.copy(alpha = 0.8f),
                          modifier = Modifier.size(20.dp)
                      )
                      Spacer(Modifier.width(12.dp))
                      Text("下一首播放", fontSize = 15.sp, color = Color.Black)
                  }
                  
                  // 3. Add to Playlist
                  Row(
                      modifier = Modifier
                          .fillMaxWidth()
                          .clickable { 
                               showSelectionMenu = false
                               showAddToPlaylist = true
                          }
                          .padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      androidx.compose.material3.Icon(
                          imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, 
                          contentDescription = "添加到歌单", 
                          tint = Color.Black.copy(alpha = 0.8f),
                          modifier = Modifier.size(20.dp)
                      )
                      Spacer(Modifier.width(12.dp))
                      Text("添加到歌单", fontSize = 15.sp, color = Color.Black)
                  }
                  
                  // 4. Delete
                  Row(
                      modifier = Modifier
                          .fillMaxWidth()
                          .clickable { 
                               showSelectionMenu = false
                               showDeleteConfirm = true
                          }
                          .padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      androidx.compose.material3.Icon(
                          imageVector = Icons.Default.Delete, 
                          contentDescription = "删除", 
                          tint = Color.Black.copy(alpha = 0.8f),
                          modifier = Modifier.size(20.dp)
                      )
                      Spacer(Modifier.width(12.dp))
                      Text("删除", fontSize = 15.sp, color = Color.Black)
                  }
              }
        }

        // Hoisted Repeat Menu
        val repeatMode by audioViewModel.repeatMode.collectAsState()
        com.vagueplayer.music.ui.components.RepeatModeMenu(
            isExpanded = showRepeatMenu,
                onDismiss = { 
                    showRepeatMenu = false
                    overlayBounds = null
                },
                anchorSize = androidx.compose.ui.unit.DpSize(50.dp, 50.dp),
                anchorPosition = repeatMenuAnchor,

                hazeState = mainHazeState,
                onLayoutCoordinates = { overlayBounds = it.boundsInRoot() },
                currentMode = repeatMode,
                onModeSelected = { mode -> 
                    if (mode == 3) {
                         audioViewModel.setShuffleMode(true)
                    } else {
                         audioViewModel.toggleRepeatMode(mode)
                         if (mode != 0) audioViewModel.setShuffleMode(false) // 0 = OFF
                    }
                    showRepeatMenu = false 
                    overlayBounds = null
                },
                onSetCount = { 
                    showRepeatMenu = false
                    showLoopCountDialog = true
                    // showLoopCountDialog will take over overlayBounds
                },

        )



        if (showLoopCountDialog) {
             com.vagueplayer.music.ui.components.GlassDialog(
                 hazeState = mainHazeState,
                 title = "设置循环次数",
                 onDismissRequest = {  
                     showLoopCountDialog = false
                     overlayBounds = null
                 },
                 confirmText = "确认",
                 cancelText = "取消",
                 onLayoutCoordinates = { overlayBounds = it.boundsInRoot() },
                 onConfirm = {
                      val count = tempLoopCount.toIntOrNull() ?: 0
                      audioViewModel.setLoopCount(count)
                      showLoopCountDialog = false
                      overlayBounds = null
                      tempLoopCount = "" // Reset
                 },
                 content = {
                      Column(
                          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                          horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                          OutlinedTextField(
                              value = tempLoopCount,
                              onValueChange = { str ->
                                  if (str.all { it.isDigit() }) {
                                      tempLoopCount = str 
                                  }
                              },
                              label = { Text("输入次数 (如: 5)") },
                              singleLine = true,
                              modifier = Modifier.fillMaxWidth(),
                              keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                              ),
                              shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                              colors = OutlinedTextFieldDefaults.colors(
                                  focusedBorderColor = com.vagueplayer.music.ui.theme.AccentBlue,
                                  unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                  focusedLabelColor = com.vagueplayer.music.ui.theme.AccentBlue,
                                  cursorColor = com.vagueplayer.music.ui.theme.AccentBlue
                              )
                          )
                      }
                 }
             )
        }



        // Song Action Menu
        com.vagueplayer.music.ui.components.SongActionMenu(
            isExpanded = showSongMenu,
            onDismiss = { 
                showSongMenu = false
                overlayBounds = null
            },
            anchorPosition = songMenuAnchor,
            anchorSize = songMenuSize,
            song = activeSongForMenu,
            onPlayNext = { song ->
                audioViewModel.addToNext(listOf(song))
            },
            onAddToPlaylist = { song ->
                activeSongForMenu = song
                showAddToPlaylist = true
            },
            onDelete = { song ->
                activeSongForMenu = song
                showDeleteConfirm = true
            },
            onLayoutCoordinates = { overlayBounds = it.boundsInRoot() }
        )

        // 4. Settings Screen (Full Screen Overlay)
        // 4. Settings Screen (Full Screen Overlay)
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .zIndex(500f) // Above everything
                .fillMaxSize()
        ) {
            SettingsScreen(
                onBack = { showSettings = false },
                viewModel = audioViewModel
            )
        }





        // Global Playlist Delete Dialog (Hoisted)
        val playlistToDelete by audioViewModel.playlistToDelete.collectAsState()
        if (playlistToDelete != null) {
            com.vagueplayer.music.ui.components.GlassAlertDialog(
                title = "删除歌单",
                description = "确定要删除歌单 '${playlistToDelete!!.name}' 吗？",
                icon = Icons.Default.Delete,
                confirmText = "删除",
                cancelText = "取消",
                hazeState = mainHazeState,
                onConfirm = {
                    audioViewModel.confirmDeletePlaylist()
                    overlayBounds = null
                },
                onDismiss = { 
                    audioViewModel.cancelDeletePlaylist()
                    overlayBounds = null
                }
            )
        }

        }

        // -------------------------------------------------------------------------
        // SINK LAYER: Unified Glass Dock (Topmost Layer)
        // -------------------------------------------------------------------------
        
        // -------------------------------------------------------------------------
        // SINK LAYER: Unified Glass Dock (Topmost Layer)
        // -------------------------------------------------------------------------
        
        // Determine if we are in a sub-screen where Dock should be "MiniPlayer Only" (No Navigation)
        val isSubScreen = selectedPlaylist != null || showSettings 
        
        // Always mounted for Shared Element validity
        AnimatedVisibility(
            visible = !showPlayer,
            enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),

            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(100f)
        ) {
             Box(
                 modifier = Modifier
                    .graphicsLayer { 
                        // Block sinking if in sub-screen
                        val shouldSink = !isDockVisible && !isSubScreen
                        val dockAlpha = if (!shouldSink) 1f else 0f 
                        alpha = dockAlpha
                        translationY = (1f - dockAlpha) * 100f
                    }
                    .fillMaxWidth()
             ) {
                 // Reset bounds when entering sub-screen to prevent ghost distortion
                 LaunchedEffect(isSubScreen) {
                     if (isSubScreen) {
                        navBounds = androidx.compose.ui.geometry.Rect.Zero
                        searchBounds = androidx.compose.ui.geometry.Rect.Zero
                     }
                 }

                  // Hide Dock when Player is Open

                    com.vagueplayer.music.ui.components.UnifiedGlassDock(
                        modifier = Modifier.fillMaxWidth(),
                        showNavigation = !isSubScreen, // Hide Bottom Bar in sub-screens
                        onPlayerPositioned = { playerBounds = it },
                        onNavPositioned = { navBounds = it },
                        onSearchPositioned = { searchBounds = it },
                        availableWidth = LocalConfiguration.current.screenWidthDp.dp,
                        collapseProgress = if (isSubScreen) 0f else effectiveCollapseProgress,
                        onExpandDock = { isDockCollapsed = false },
                        onExpandPlayer = { showPlayer = true },
                        onSearchClick = { 
                            if (isDockCollapsed) isDockCollapsed = false
                            else isSearchActive = !isSearchActive 
                        },
                        isSelectionMode = isSelectionMode,
                        playerContainerModifier = Modifier
                             .zIndex(1f)
                             .transformSource(
                                 key = "container_transform", 
                                 sharedTransitionScope = this@SharedTransitionLayout,
                                 animatedVisibilityScope = this@AnimatedVisibility,
                                 renderInOverlay = false // Prevent it from drawing over PlayerScreen when hidden
                             ),
                        playerContent = {
                            MiniPlayer(
                                viewModel = audioViewModel,
                                modifier = Modifier.fillMaxSize(),
                                collapseProgress = if (isSubScreen) 0f else effectiveCollapseProgress,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onSongMenuRequest = { song, offset, size ->
                                    activeSongForMenu = song
                                    songMenuAnchor = offset
                                    size?.let { songMenuSize = it } ?: run { songMenuSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp) }
                                    showSongMenu = true
                                },
                                onExpand = { showPlayer = true },
                                onPlaylistClick = { 
                                    if (isSelectionMode) showSelectionMenu = true
                                    else if (isDockVisible) showPlaylistGlobal = true
                                },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedVisibility
                            )
                        },
                        navContent = { expandedWidth ->
                             val showSubScreenNav = showSettings || selectedPlaylist != null || isSelectionMode
                             
                             // Dynamic Navigation Items for Selection Mode
                             // If collapsed (circle) -> Show "Menu" (Three Dots)
                             // If expanded (pill) -> Show "Actions" (Delete, etc.)
                             val currentNavItems = if (isSelectionMode) {
                                  if (effectiveCollapseProgress > 0.5f) {
                                      // Collapsed: Show "More"
                                      listOf(com.vagueplayer.music.ui.components.NavItem("Menu", androidx.compose.material.icons.Icons.Default.MoreVert))
                                  } else {
                                      // Expanded: Show Actions directly
                                      listOf(
                                          com.vagueplayer.music.ui.components.NavItem("Delete", androidx.compose.material.icons.Icons.Default.Delete),
                                          com.vagueplayer.music.ui.components.NavItem("PlayNext", androidx.compose.material.icons.Icons.AutoMirrored.Filled.PlaylistPlay),
                                          com.vagueplayer.music.ui.components.NavItem("AddPlaylist", androidx.compose.material.icons.Icons.AutoMirrored.Filled.PlaylistAdd)
                                      )
                                  }
                             } else if (showSettings || selectedPlaylist != null) {
                                  listOf(
                                      com.vagueplayer.music.ui.components.NavItem("Back", androidx.compose.material.icons.Icons.Default.Close),
                                      com.vagueplayer.music.ui.components.NavItems[1], 
                                      com.vagueplayer.music.ui.components.NavItems[2]
                                  )
                             } else {
                                  com.vagueplayer.music.ui.components.NavItems
                             }
                             
                             com.vagueplayer.music.ui.components.AnimatedGlassNavBar(
                                 items = currentNavItems,
                                 selectedIndex = if (showSubScreenNav) 0 else currentPage,
                                 onItemSelected = { index -> 
                                     if (isSelectionMode) {
                                         if (effectiveCollapseProgress > 0.5f) {
                                             // Menu Click (Collapsed) -> Expand Dock
                                             isDockCollapsed = false 
                                         } else {
                                             // Action Click (Expanded)
                                             when (index) {
                                                 0 -> showDeleteConfirm = true
                                                 1 -> {
                                                     val selectedSongs = songs.filter { it.id in selectedIds }
                                                     audioViewModel.addToNext(selectedSongs)
                                                     audioViewModel.clearSelection()
                                                 }
                                                 2 -> showAddToPlaylist = true
                                             }
                                         }
                                     } else if (showSubScreenNav && index == 0) {
                                         if (showSettings) showSettings = false
                                         else if (selectedPlaylist != null) selectedPlaylist = null
                                     } else {
                                         currentPage = index
                                     }
                                 },
                                 collapseProgress = effectiveCollapseProgress,
                                 expandedWidth = expandedWidth
                             )
                        },
                        searchContent = {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    )

             }
        }

    // Back Handlers (LIFO - Last Defined = First Handled)
    
    // 1. Base Navigation (Go to Home) - Lowest Priority
    BackHandler(enabled = currentPage != 0 && !showPlayer && !isSearchActive && !isAnyOverlayVisible) { 
        currentPage = 0 
    }

    // 2. Search Mode
    BackHandler(enabled = isSearchActive && !isAnyOverlayVisible) { isSearchActive = false }

    // 3. Immersive Player 
    // Disable this handler if ANY internal overlays (like Repeat Menu) are open
    BackHandler(enabled = showPlayer && !isAnyOverlayVisible) {  
        showPlayer = false 
        overlayBounds = null
    }

    // 4. Full Screen Overlay Screens
    androidx.compose.animation.AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                 fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "SettingsTransition",
            modifier = Modifier.zIndex(500f) 
    ) { isVisible ->
        if (isVisible) {
             com.vagueplayer.music.ui.screens.SettingsScreen(
                 viewModel = audioViewModel,
                 onBack = { showSettings = false },
                 hazeState = mainHazeState,
                 modifier = Modifier
                    .fillMaxSize()

                    // Shared Bounds with MiniPlayer (or Dock) as source
                    // Assuming MiniPlayer key is "mini_player_container" or similar if we want true morph.
                    // For now, using "settings_card" which is linked to Profile. 
                    // To "mimic MiniPlayer expansion", we might need a transformSource from the Dock.
                    // But if Profile is the entry point, stick to "settings_card" but with correct physics.
                    // Removed .transformSource helper. 
                    // SharedBounds is now applied INSIDE SettingsScreen.kt to the root content.
                    // This prevents "Double Animation" conflict.
             )
        }
    }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showRecentOverlay) { showRecentOverlay = false }
    BackHandler(enabled = showFavoritesOverlay) { showFavoritesOverlay = false }

    // 5. Dialogs
    BackHandler(enabled = showDeleteConfirm) { 
        showDeleteConfirm = false
        overlayBounds = null
    }
    BackHandler(enabled = showCreatePlaylistDialog) { 
        showCreatePlaylistDialog = false
        overlayBounds = null
    }
    BackHandler(enabled = showAddToPlaylist) { showAddToPlaylist = false }

    // 6. Menus (High Priority)
    BackHandler(enabled = showPlaylistMenu) { 
        showPlaylistMenu = false
        overlayBounds = null
    }
    BackHandler(enabled = showRepeatMenu) { 
        showRepeatMenu = false
        overlayBounds = null
    }
    BackHandler(enabled = showSortMenu) { 
        showSortMenu = false
        overlayBounds = null
    }
    BackHandler(enabled = showSongMenu) { 
        showSongMenu = false
        overlayBounds = null
    }
    
    // Playlist Global Overlay (Highest Priority for Overlay Stack)
    // Moved here to ensure it closes BEFORE PlayerScreen if both are open.
// Playlist Global Overlay (Highest Priority for Overlay Stack)
    // Directly render; component handles internal AnimatedVisibility
    // Playlist Global Overlay (Highest Priority for Overlay Stack)
    // Directly render; component handles internal AnimatedVisibility
    com.vagueplayer.music.ui.components.GlassPlaylistOverlay(
         viewModel = audioViewModel,
         isVisible = showPlaylistGlobal, 
         onDismiss = { showPlaylistGlobal = false }
    )
    BackHandler(enabled = showPlaylistGlobal) { showPlaylistGlobal = false }
    } // End Root Box (contains both Haze Box and Dialog Layer as siblings)
    } // End SharedTransitionLayout
} // End MainScreen
