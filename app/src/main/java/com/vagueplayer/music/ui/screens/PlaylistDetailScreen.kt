package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border // [FIX] Import border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.core.graphics.drawable.toBitmap // [FIX] Import toBitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // [FIX] Import Edit Icon
import androidx.compose.material.icons.filled.Menu // [FIX] Import Menu Icon
import androidx.compose.material.icons.filled.MoreVert // [FIX] Added missing import
import androidx.compose.foundation.lazy.itemsIndexed // [FIX] Import itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.* // [FIX] expanded to * for remember, mutableState
import androidx.compose.runtime.saveable.rememberSaveable // [FIX] Import
import androidx.activity.compose.PredictiveBackHandler // [FIX] Import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // [FIX] Import
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource // [FIX] Import
import androidx.compose.ui.draw.clip // [FIX] Import
import androidx.compose.ui.text.font.FontWeight // [FIX] Import
import androidx.compose.ui.text.style.TextOverflow // [FIX] Import
import androidx.compose.foundation.shape.RoundedCornerShape // [FIX] Import
import androidx.compose.ui.unit.sp // [FIX] Import
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel
import kotlinx.coroutines.flow.collect
import com.vagueplayer.music.ui.components.simpleGlass // [FIX] Import simpleGlass
import androidx.compose.foundation.interaction.MutableInteractionSource // [FIX] Import
import androidx.compose.ui.layout.onGloballyPositioned // [FIX] Import
import androidx.compose.ui.layout.boundsInWindow // [FIX] Import
import androidx.compose.material.icons.automirrored.filled.Sort // [FIX] Import
import androidx.compose.material.icons.filled.Check // [FIX] Import
import androidx.compose.ui.layout.positionInWindow // [FIX] Import
import com.vagueplayer.music.ui.components.liquidGlassLens // [FIX] Import
import com.vagueplayer.music.ui.components.GlassIconButton // [FIX] Import
import androidx.palette.graphics.Palette // [NEW] Import
import android.graphics.Bitmap // [NEW] Import
import androidx.core.graphics.drawable.toBitmap // [NEW] Import

import androidx.compose.runtime.rememberCoroutineScope // [NEW] Import
import kotlinx.coroutines.launch // [NEW] Import
import androidx.compose.material3.ExperimentalMaterial3Api // [FIX] Import if needed

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: AudioViewModel,
    onDismissRequest: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    hazeState: HazeState? = null,
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> } // [NEW] Callback for menu
) {
    // [FIX] Use effective state (from MainScreen or local fallback)
    val effectiveHazeState = hazeState ?: remember { HazeState() }

    // Predictive Back State
    var backProgress by remember { mutableFloatStateOf(0f) }
    val scale = 1f - (backProgress * 0.1f)
    val alpha = 1f - (backProgress * 0.2f)
    val yOffset = backProgress * 100f

    androidx.activity.compose.PredictiveBackHandler { progress ->
        try {
            progress.collect { event -> backProgress = event.progress }
            onDismissRequest()
        } catch (e: java.util.concurrent.CancellationException) {
            backProgress = 0f
        } finally {
            backProgress = 0f
        }
    }

    // State
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name) }
    
    // Sort Logic - [FIX] Observe Per-Playlist Sort Option
    val playlistSortOptions by viewModel.playlistSortOptions.collectAsState()
    val currentSortOption = playlistSortOptions[playlist.id] ?: com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.TITLE // Default to Title if not set

    // [FIX] Selection State (Hoisted to Top Level)
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    val sortedSongs = remember(playlist.songs, currentSortOption) {
        if (currentSortOption == com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.CUSTOM) playlist.songs 
        else viewModel.sortSongs(playlist.songs, currentSortOption)
    }
    
    // Menus
    var showActionMenu by remember { mutableStateOf(false) } // Renamed from showSortMenu
    var showRealSortMenu by remember { mutableStateOf(false) } // New Sort Menu
    
    // [FRAMEWORK] Glass Lens Coordinates (Global Root Relative)
    var rootLayoutCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var backButtonBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var sortButtonBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var menuBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) } // [NEW] Menu Bounds

    // [CLEANUP] Clear menu bounds after animation to prevent ghost distortion
    LaunchedEffect(showRealSortMenu) {
        if (!showRealSortMenu) {
            kotlinx.coroutines.delay(400) // Wait for exit animation (350ms)
            menuBounds = androidx.compose.ui.geometry.Rect.Zero
        }
    }

    // Anchors
    var actionMenuPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var sortMenuAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // [FRAMEWORK] Tint Logic
    var backIconTint by remember { mutableStateOf(Color.White) }
    var sortIconTint by remember { mutableStateOf(Color.White) }

    // Helper to calculate bounds relative to root
    fun calculateBounds(child: androidx.compose.ui.layout.LayoutCoordinates, root: androidx.compose.ui.layout.LayoutCoordinates?): androidx.compose.ui.geometry.Rect {
        return if (root != null && root.isAttached && child.isAttached) {
            val rootPos = root.positionInWindow()
            val childPos = child.positionInWindow()
            androidx.compose.ui.geometry.Rect(
                left = childPos.x - rootPos.x,
                top = childPos.y - rootPos.y,
                right = childPos.x - rootPos.x + child.size.width,
                bottom = childPos.y - rootPos.y + child.size.height
            )
        } else {
            androidx.compose.ui.geometry.Rect.Zero
        }
    }

    // Scroll State for Alpha [NEW]
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 300f).coerceIn(0f, 1f)
        }
    }


    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                translationY = yOffset
            },
        color = Color.Transparent // [FIX] Let container handle background
    ) {
        // Root Container - THIS is the shared element destination
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "container_${playlist.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp)),
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                )
                .background(MaterialTheme.colorScheme.background) // [FIX] Background moves here
                .onGloballyPositioned { rootLayoutCoordinates = it }
        ) {
            // [LAYER 1] Content with Liquid Lens Distortion
            // The Lens is applied here, distorting the LazyColumn content.
            Box(
                modifier = Modifier
                    .fillMaxSize() // [FIX] Removed .haze() - causes ghosting after navigation
                    .liquidGlassLens(
                        bounds1 = backButtonBounds,
                        bounds2 = sortButtonBounds,
                        bounds3 = menuBounds, // [NEW] Connect Menu to Lens System
                        distortionStrength = 45f,
                        edgeWidth = 30f,
                        tint = Color.White.copy(alpha = 0.05f)
                    )
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(effectiveHazeState), // [FIX] Mark List as Source
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // 1. Header Image Item
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            val coverUri = playlist.songs.firstOrNull()?.albumArtUri
                            val context = LocalContext.current
                            
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(coverUri)
                                    .crossfade(false)
                                    .allowHardware(false) 
                                    .placeholderMemoryCacheKey("cover_${playlist.id}")
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                onSuccess = { state ->
                                    // [STRATEGY] Dual-Zone Sampling based on bitmap corners
                                    // This logic assumes 0,0 is top-left of image, which is consistent.
                                    val bitmap = state.result.drawable.toBitmap()
                                    // ... (Keeping simplified tint logic or reusing previous logic if needed, 
                                    // but for reliability, let's just stick to basic corner sampling for now 
                                    // since we removed the bounds-based complex sampling to fix the error).
                                    // Actually, let's perform a simple check:
                                    
                                    fun getLuminance(x: Int, y: Int, w: Int, h: Int): Double {
                                        if (x + w > bitmap.width || y + h > bitmap.height) return 0.0
                                        val p = IntArray(w * h)
                                        bitmap.getPixels(p, 0, w, x, y, w, h)
                                        var lumSum = 0.0
                                        for (c in p) {
                                            lumSum += androidx.core.graphics.ColorUtils.calculateLuminance(c)
                                        }
                                        return lumSum / p.size
                                    }

                                    // Simple Corner Sampling (Top-Left 20% and Top-Right 20%)
                                    val w = bitmap.width
                                    val h = bitmap.height
                                    val boxW = (w * 0.2).toInt()
                                    val boxH = (h * 0.15).toInt()
                                    
                                    val lumL = getLuminance(0, 0, boxW, boxH)
                                    val lumR = getLuminance(w - boxW, 0, boxW, boxH)
                                    
                                    backIconTint = if (lumL > 0.45) Color.Black.copy(alpha = 0.87f) else Color.White
                                    sortIconTint = if (lumR > 0.45) Color.Black.copy(alpha = 0.87f) else Color.White
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    // [FIX] Ensure Image Fills Container
                                    .fillMaxSize()
                            )
                        }
                    }
    
                    // 2. Title & Metadata
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { 
                                    renameText = playlist.name
                                    showRenameDialog = true 
                                }
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f, fill = false) 
                                )
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp).padding(start = 8.dp), tint = Color.Gray)
                            }
                            Text(
                                text = "${playlist.songs.size} 首歌",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // 3. Song List
                    itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                        com.vagueplayer.music.ui.screens.SongItem(
                            song = song,
                            selectedIds = selectedIds,
                            isSelectionMode = isSelectionMode,
                            viewModel = viewModel,
                            onClick = { clickedSong ->
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(clickedSong.id)
                                } else {
                                    viewModel.playSong(clickedSong, sortedSongs, playlist.name)
                                }
                            },
                            onLongClick = { clickedSong ->
                                if (!isSelectionMode) {
                                    viewModel.toggleSelection(clickedSong.id)
                                }
                            },
                            onMenuClick = { s, offset ->
                                onSongMenuRequest(s, offset, null)
                            }
                        )
                    }
                }
            }

            // Alphabet Sidebar
            val isSidebarLeft by viewModel.isSidebarOnLeft.collectAsState()
            val sidebarSections = remember { (listOf('#') + ('A'..'Z')).toList() }
            val coroutineScope = rememberCoroutineScope()
            
            // Only show sidebar if sorting by TITLE or ARTIST
            if (sortedSongs.isNotEmpty() && (currentSortOption == com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.TITLE || currentSortOption == com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.ARTIST)) {
                com.vagueplayer.music.ui.components.AlphabetSideBar(
                    sections = sidebarSections,
                    onLetterSelected = { letter ->
                        // Find first song starting with this letter
                        val targetIndex = sortedSongs.indexOfFirst { song ->
                            val firstChar = song.title.firstOrNull()?.uppercaseChar()
                            if (letter == '#') {
                                firstChar != null && !firstChar.isLetter()
                            } else {
                                firstChar == letter
                            }
                        }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                // +2 offset because of Header Image (index 0) and Metadata (index 1)
                                listState.animateScrollToItem(targetIndex + 2)
                            }
                        }
                    },
                    isOnLeft = isSidebarLeft,
                    modifier = Modifier
                        .align(if (isSidebarLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(
                            start = if (isSidebarLeft) 8.dp else 0.dp,
                            end = if (isSidebarLeft) 0.dp else 8.dp
                        )
                )
            }

            // [LAYER 2] Floating Header (Consolidated)
            com.vagueplayer.music.ui.components.ScreenHeader(
                title = if (scrollAlpha > 0.5f) playlist.name else "", // Fade title in later
                scrollAlpha = scrollAlpha,
                contentColor = if (scrollAlpha > 0.5f) Color.Black else backIconTint, // Transition to Black on Glass
                // glassTint = Color.White.copy(alpha = 0.85f), // [FIX] Removed to use default 0.4f
                hazeState = effectiveHazeState, // [FIX] Use effective HazeState
                navigationIcon = {
                    GlassIconButton(
                        onClick = onDismissRequest,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (scrollAlpha > 0.5f) Color.Black else backIconTint,
                        glassTint = Color.White.copy(alpha = 0.2f), // [FIX] Visible glass background
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                 backButtonBounds = calculateBounds(coordinates, rootLayoutCoordinates)
                            }
                    )
                },
                action = {
                    GlassIconButton(
                        onClick = { showRealSortMenu = true },
                        icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = if (scrollAlpha > 0.5f) Color.Black else sortIconTint,
                        glassTint = Color.White.copy(alpha = 0.2f), // [FIX] Visible glass background
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val bounds = calculateBounds(coordinates, rootLayoutCoordinates)
                                sortButtonBounds = bounds
                                sortMenuAnchor = bounds.topLeft
                            }
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            )
        }

        // [LAYER 3] Overlays
        
        // 2. [NEW] Sort Menu
        com.vagueplayer.music.ui.components.MorphingGlassMenu(
            isExpanded = showRealSortMenu,
            onDismiss = { showRealSortMenu = false },
            anchorSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp), // [RESIZE] Updated to 48dp
            anchorPosition = sortMenuAnchor, 
            hazeState = effectiveHazeState, // [FIX] Haze Support
            onLayoutCoordinates = { calculateBounds(it, rootLayoutCoordinates).let { rect -> menuBounds = rect } }
        ) {
              val currentSort = currentSortOption // [FIX] Use Local Context Option
              val options: List<Pair<String, com.vagueplayer.music.viewmodel.AudioViewModel.SortOption>> = listOf(
                  "标题" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.TITLE,
                  "艺术家" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.ARTIST,
                  "大小" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.SIZE,
                  "播放次数" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.PLAY_COUNT,
                  "时长 (短→长)" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.DURATION_ASC,
                  "时长 (长→短)" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.DURATION_DESC,
                  "添加时间" to com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.DATE_ADDED
              )
              
              Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                  options.forEach { (label, option) ->
                      val isSelected = currentSort == option
                      Row(
                           modifier = Modifier
                               .fillMaxWidth()
                               .clickable { 
                                   viewModel.setSortOption(option, playlist.id) // [FIX] Set for THIS Playlist ID
                                   // viewModel.resortSongs() // [FIX] No need to resort Global Library
                                   showRealSortMenu = false
                               }
                               .padding(vertical = 8.dp, horizontal = 20.dp), // [MATCH] MainScreen Padding
                           horizontalArrangement = Arrangement.SpaceBetween,
                           verticalAlignment = Alignment.CenterVertically
                      ) {
                          Text(
                              text = label, 
                              color = if(isSelected) com.vagueplayer.music.ui.theme.AccentBlue else Color.Black, 
                              fontSize = 15.sp, 
                              fontWeight = if(isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                          )
                          
                          // [MATCH] Custom Circle from MainScreen (Not RadioButton)
                          Box(
                              modifier = Modifier
                                  .size(20.dp)
                                  .border(
                                      width = 2.dp, 
                                      color = if (isSelected) com.vagueplayer.music.ui.theme.AccentBlue else Color.Gray.copy(alpha = 0.5f), 
                                      shape = androidx.compose.foundation.shape.CircleShape
                              ),
                              contentAlignment = Alignment.Center
                          ) {
                              if (isSelected) {
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
        }
        
        if (showRenameDialog) {
            com.vagueplayer.music.ui.components.GlassDialog(
                onDismissRequest = { showRenameDialog = false },
                title = "重命名歌单",
                content = {
                    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                    
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("歌单名称") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.vagueplayer.music.ui.theme.AccentBlue,
                            cursorColor = com.vagueplayer.music.ui.theme.AccentBlue
                        )
                    )
                },
                confirmText = "保存",
                onConfirm = {
                    viewModel.renamePlaylist(playlist.id, renameText)
                    showRenameDialog = false
                },
                cancelText = "取消",
                onCancel = { showRenameDialog = false }
            )
        }
    }
}
