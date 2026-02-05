package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel
import kotlinx.coroutines.flow.collect

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.material.icons.automirrored.filled.Sort

import androidx.compose.ui.layout.positionInWindow

import com.vagueplayer.music.ui.components.GlassIconButton
import com.vagueplayer.music.ui.components.liquidGlassLens


import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: AudioViewModel,
    onDismissRequest: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    hazeState: HazeState? = null,
    playerBounds: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero, // For glass distortion
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> }
) {
    val effectiveHazeState = hazeState ?: remember { HazeState() }

    // Predictive Back State
    // Standard Back Handler to ensure reliable exit
    androidx.activity.compose.BackHandler(onBack = onDismissRequest)

    // State
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name) }
    
    // Sort Logic
    val playlistSortOptions by viewModel.playlistSortOptions.collectAsState()
    val currentSortOption = playlistSortOptions[playlist.id] ?: com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.TITLE

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    val sortedSongs = remember(playlist.songs, currentSortOption) {
        if (currentSortOption == com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.CUSTOM) playlist.songs 
        else viewModel.sortSongs(playlist.songs, currentSortOption)
    }
    
    // Menus
    var showActionMenu by remember { mutableStateOf(false) }
    var showRealSortMenu by remember { mutableStateOf(false) }
    
    var rootLayoutCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var backButtonBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var sortButtonBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var menuBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    LaunchedEffect(showRealSortMenu) {
        if (!showRealSortMenu) {
            kotlinx.coroutines.delay(400) // Wait for exit animation
            menuBounds = androidx.compose.ui.geometry.Rect.Zero
        }
    }

    // Anchors
    var actionMenuPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var sortMenuAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    var backIconTint by remember { mutableStateOf(Color.White) }
    var sortIconTint by remember { mutableStateOf(Color.White) }

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

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 300f).coerceIn(0f, 1f)
        }
    }


    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color.Transparent
    ) {
        // Glass Lens Wrapper for content distortion
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassLens(
                    bounds1 = playerBounds,
                    bounds2 = null,
                    bounds3 = null,
                    bounds4 = null,
                    distortionStrength = 45f,
                    edgeWidth = 60f,
                    fusionStrength = 35f,
                    aberrationStrength = 0.3f,
                    tint = Color.White.copy(alpha = 0.80f),
                    enableShader = true
                )
        ) {
        // Content Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootLayoutCoordinates = it }
        ) {
            // Haze Source Container - Wraps Background and List
            // Separated from Overlay Elements (Header, Sidebar) to prevent Haze recursion
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .haze(effectiveHazeState)
            ) {
                // Shared Background Layer - Decoupled from Content
                    // Shared Background Layer - Decoupled from Content
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        // .sharedBounds(...) REMOVED to fix flicker/glitch
                        .background(MaterialTheme.colorScheme.background)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        // 1. Header Image
                        // 1. Header Image
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .sharedElement(
                                        state = rememberSharedContentState(key = "cover_${playlist.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) }
                                    )
                            ) {
                                // Background Blur Image
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(playlist.songs.firstOrNull()?.albumArtUri)
                                        .crossfade(false)
                                        .memoryCacheKey("cover_${playlist.id}")
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                )
                            }
                        }
        
                        // 2. Title & Metadata
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent) // Explicit fix for potential white block
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
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false) 
                                    )
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp).padding(start = 8.dp), tint = Color.Gray)
                                }
                                
                                // Metadata Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "${playlist.songs.size} 首歌",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                    
                                    // [NEW] Refresh Time for Daily Recommendations
                                    if (playlist.id == "daily_recommend") {
                                        val recommendationState by viewModel.recommendationState.collectAsState()
                                        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        val timeString = timeFormat.format(java.util.Date(recommendationState.lastRefreshTime))
                                        
                                        Text(
                                            text = " • 刷新于 $timeString",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Thin,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.Gray.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
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
                        android.util.Log.d("PlaylistSidebar", "=== Letter: $letter ===")
                        
                        // Find first song starting with this letter (always use title, like LibraryScreen)
                        val targetIndex = sortedSongs.indexOfFirst { song ->
                            val firstChar = song.title.firstOrNull()?.uppercaseChar()
                            val matches = if (letter == '#') {
                                firstChar != null && !firstChar.isLetter()
                            } else {
                                firstChar == letter
                            }
                            
                            // Debug logging for letter A
                            if (letter == 'A' && song.title.firstOrNull()?.uppercaseChar() in listOf('A', 'a')) {
                                android.util.Log.d("PlaylistSidebar", "Checking A: '${song.title}' firstChar=$firstChar matches=$matches")
                            }
                            
                            matches
                        }
                        android.util.Log.d("PlaylistSidebar", "Target: $targetIndex → Scroll: ${targetIndex + 2}")
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
                        .zIndex(2f) // Float above liquidGlassLens to receive touch events
                        .padding(
                            start = if (isSidebarLeft) 8.dp else 0.dp,
                            end = if (isSidebarLeft) 0.dp else 8.dp
                        )
                )
            }

            // Floating Header
            com.vagueplayer.music.ui.components.ScreenHeader(
                title = if (scrollAlpha > 0.5f) playlist.name else "",
                scrollAlpha = scrollAlpha,
                contentColor = if (scrollAlpha > 0.5f) Color.Black else backIconTint,
                hazeState = effectiveHazeState,
                navigationIcon = {
                    GlassIconButton(
                        onClick = onDismissRequest,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (scrollAlpha > 0.5f) Color.Black else backIconTint,
                        glassTint = Color.White.copy(alpha = 0.2f),
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
                        glassTint = Color.White.copy(alpha = 0.2f),
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

        // Sort Menu
        com.vagueplayer.music.ui.components.MorphingGlassMenu(
            isExpanded = showRealSortMenu,
            onDismiss = { showRealSortMenu = false },
            anchorSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
            anchorPosition = sortMenuAnchor, 
            hazeState = effectiveHazeState,
            onLayoutCoordinates = { calculateBounds(it, rootLayoutCoordinates).let { rect -> menuBounds = rect } }
        ) {
              val currentSort = currentSortOption
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
                                   viewModel.setSortOption(option, playlist.id)
                                   showRealSortMenu = false
                               }
                               .padding(vertical = 8.dp, horizontal = 20.dp),
                           horizontalArrangement = Arrangement.SpaceBetween,
                           verticalAlignment = Alignment.CenterVertically
                      ) {
                          Text(
                              text = label, 
                              color = if(isSelected) com.vagueplayer.music.ui.theme.AccentBlue else Color.Black, 
                              fontSize = 15.sp, 
                              fontWeight = if(isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                          )
                          
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
                hazeState = effectiveHazeState,
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
        } // End liquidGlassLens Box
    } // End Surface
}
