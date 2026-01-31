package com.vagueplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.vagueplayer.music.ui.theme.AccentBlue
import androidx.compose.foundation.border
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vagueplayer.music.viewmodel.AudioViewModel
import com.vagueplayer.music.viewmodel.AudioViewModelFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import com.vagueplayer.music.ui.components.simpleGlass

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onShowSortOptions: (Offset) -> Unit,
    hazeState: HazeState? = null,
    onSongMenuRequest: (com.vagueplayer.music.data.model.Song, androidx.compose.ui.geometry.Offset, androidx.compose.ui.unit.DpSize?) -> Unit = { _, _, _ -> }
) {
    // Define Local State for Button Position
    var sortButtonPosition by remember { mutableStateOf(Offset.Zero) }

    val context = LocalContext.current
    val factory = AudioViewModelFactory(context)
    val viewModel: AudioViewModel = viewModel(factory = factory)

    val songs by viewModel.songs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // ViewModel Selection State
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    val customFolders by viewModel.customFolders.collectAsState()
    
    // Folder Picker Launcher
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.addCustomFolder(it)
        }
    }

    // Auto-Exit: Handle Back Press to exit selection mode
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isScanning,
        onRefresh = { 
            if (customFolders.isEmpty()) {
                launcher.launch(null)
            } else {
                viewModel.scanMedia() 
            }
        }
    )

    // Scroll State
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Use Global HazeState if provided to ensure sync with MainScreen.
    // Fallback to local state ONLY if null (e.g. standard-alone preview)
    val effectiveHazeState = hazeState ?: remember { HazeState() }
    
    // Calculate Header Alpha based on scroll
    val scrollAlpha = remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            
            // Start fading in after 50dp scroll
            val threshold = 50f
            val scrollY = firstVisibleItemIndex * 80f + firstVisibleItemScrollOffset // Approximate item height
            
            (scrollY / threshold).coerceIn(0f, 1f)
        }
    }.value


        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
        // CONTENT LAYER: Directly inside SOURCE Box
            // Note: Headers will be floating, so no static header here
            // Sidebar State
            val isSidebarLeft = viewModel.isSidebarOnLeft.collectAsState().value
            val sidebarSections = remember { (listOf('#') + ('A'..'Z')).toList() }

            // Container for the List (White Sheet)
            // Takes up remaining space
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = effectiveHazeState), // [MOVED] Source applied here (Content only)
                color = Color.White, // Pure White Card
                // shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // [USER REQUEST] Removed rounded corners
                tonalElevation = 0.dp // Flat
            ) {
                 Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier, // Source moved to Surface wrapper
                        state = listState,
                        contentPadding = PaddingValues(
                            top = 120.dp, // Space for floating header (96dp + gap)
                            bottom = 160.dp,
                            start = if (isSidebarLeft) 20.dp else 0.dp, 
                            end = if (!isSidebarLeft) 20.dp else 0.dp
                        )
                    ) {
                    if (songs.isEmpty() && !isScanning) {
                         item {
                             Box(modifier = Modifier.fillParentMaxSize().height(200.dp), contentAlignment = Alignment.Center)  {
                                 Text("暂无音乐，下拉刷新或检查媒体来源设置", color = Color.Gray)
                             }
                         }
                    }
                    
                    items(songs, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            selectedIds = selectedIds,
                            isSelectionMode = isSelectionMode,
                            viewModel = viewModel,
                            onClick = { s ->
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(s.id)
                                } else {
                                    viewModel.playSong(s, songs, "所有歌曲")
                                }
                            },
                            onLongClick = { s ->
                                if (!isSelectionMode) {
                                    viewModel.toggleSelection(s.id)
                                }
                            },
                            onMenuClick = { s, offset ->
                                onSongMenuRequest(s, offset, null) // Default size
                            }
                        )
                        
                        // Optional: Add Divider? (User didn't ask, but it's standard. Let's keep it clean/invisible for now like iOS)
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.Black.copy(alpha = 0.05f) 
                        )
                    }
                }
                
                // Alphabet Sidebar
                val sortOption by viewModel.sortOption.collectAsState()
                
                if (songs.isNotEmpty() && (sortOption == com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.TITLE || sortOption == com.vagueplayer.music.viewmodel.AudioViewModel.SortOption.ARTIST)) {
                    com.vagueplayer.music.ui.components.AlphabetSideBar(
                        sections = sidebarSections,
                        onLetterSelected = { letter ->
                            // Find first song starting with this letter
                            val targetIndex = songs.indexOfFirst { song ->
                                val firstChar = song.title.firstOrNull()?.uppercaseChar()
                                if (letter == '#') {
                                    firstChar != null && !firstChar.isLetter()
                                } else {
                                    firstChar == letter
                                }
                            }
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(targetIndex)
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
                } // Close Inner Box (List + Sidebar)
            } // Close Surface (White Sheet)
        // Content Layer Ends, Headers overlay on top within the same Box

        // Floating Headers (On Top of Content)
        if (isSelectionMode) {
            // Selection Mode Header
            // Selection Mode Header with Blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Color.White)
                    .align(Alignment.TopCenter)
                    .align(Alignment.TopCenter)
                    .zIndex(1f) // Ensure Header is above all content
                    // Apply Haze Blur to Selection Header
                    .hazeChild(
                        state = effectiveHazeState,
                        style = HazeStyle(
                            backgroundColor = Color.White.copy(alpha = 0.7f), 
                            tint = dev.chrisbanes.haze.HazeTint(Color.White.copy(alpha = 0.2f)), 
                            blurRadius = 30.dp,
                            noiseFactor = 0f
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "全选",
                        fontSize = 18.sp,
                        color = com.vagueplayer.music.ui.theme.AccentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            viewModel.selectAll(songs.map { it.id })
                        }
                    )
                    
                    Text(
                        text = "已选中 ${selectedIds.size} 项",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = "取消",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable { 
                            viewModel.clearSelection()
                        }
                    )
                }
            }
        } else {
            // Normal Header with Blur
            com.vagueplayer.music.ui.components.ScreenHeader(
                title = "音乐库",
                scrollAlpha = scrollAlpha,
                hazeState = effectiveHazeState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f),
                action = {
                    com.vagueplayer.music.ui.components.GlassIconButton(
                        onClick = { onShowSortOptions(sortButtonPosition) },
                        icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = Color.Black,
                        glassTint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                sortButtonPosition = coordinates.boundsInWindow().topLeft
                            }
                    )
                }
            )
        }
    
    
    // OVERLAY LAYER - Real Glass Dropdown (Custom Implementation)
    // OVERLAY LAYER - Morphing Glass Menu (Hoisted)




    PullRefreshIndicator(
        refreshing = isScanning,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter),
        backgroundColor = Color.White,
        contentColor = com.vagueplayer.music.ui.theme.AccentBlue
    )
    }
}
