package com.vagueplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border // [FIX] Added missing import
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
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay // [NEW]
import androidx.compose.material3.SwipeToDismissBox // [NEW]
import androidx.compose.material3.SwipeToDismissBoxValue // [NEW]
import androidx.compose.material3.rememberSwipeToDismissBoxState // [NEW]
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
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
import com.vagueplayer.music.ui.components.waterDropGlass
import dev.chrisbanes.haze.HazeState
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch // [FIX] Required for scrolling
import androidx.compose.foundation.layout.heightIn // Required for sidebar height constraint

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    hazeState: HazeState? = null
) {
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
            // Smart Refresh Logic:
            // 1. If no custom folders are set, prompt to add one.
            // 2. Otherwise, refresh library.
            if (customFolders.isEmpty()) {
                launcher.launch(null)
            } else {
                viewModel.scanMedia() 
            }
        }
    )

    val showSort = viewModel.showSortDialog.collectAsState().value



    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        // CONTENT LAYER (Blurred when Dialog is open)
        Box(
             modifier = Modifier
                 .fillMaxSize()
//                  .blur(if (showSort) 20.dp else 0.dp) [REMOVED] User Request
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // .padding(horizontal = 16.dp) REMOVED to allow Sidebar to reach edges
            ) {


                // Header: Normal vs Selection Mode
            if (isSelectionMode) {
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // ADDED padding
                        .padding(top = 80.dp, bottom = 16.dp),
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
            } else {
                // Custom Header with Right-Aligned Sort Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // ADDED padding
                        .padding(top = 80.dp, bottom = 16.dp), // Match ScreenHeader padding
                    horizontalArrangement = Arrangement.SpaceBetween, // Title Left, Button Right
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title (Left)
                    Text(
                        text = "音乐库", 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // Sort Button (Right)
                    Box {
                        IconButton(onClick = { viewModel.toggleSortDialog() }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = Color.Black // Dark text
                            )
                        }
                        
                        // Menu now handled by High Level Overlay
                        if (showSort) {
                            com.vagueplayer.music.ui.components.MorphingGlassMenu(
                                isExpanded = true,
                                onDismiss = { viewModel.toggleSortDialog() },
                                anchorSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
                                hazeState = null // Disable Haze to prevent crash
                            ) {
                                 val currentSort = viewModel.sortOption.collectAsState().value
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
                                                  viewModel.setSortOption(option)
                                                  viewModel.toggleSortDialog()
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
                                         
                                         // [REVISED] Radio Button Style Indicator
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
                        }
                    }
                }
            }


            // Song List
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            
            // Sidebar State
            val isSidebarLeft = viewModel.isSidebarOnLeft.collectAsState().value
            val sidebarSections = remember { (listOf('#') + ('A'..'Z')).toList() }
            
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        bottom = 160.dp,
                        // [RESIZE] Base padding reduced from 16.dp to 0.dp to fix "large gap"
                        // Only add sidebar width (20.dp) compensation
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
                    
                    items(songs) { song ->
                        val isSelected = selectedIds.contains(song.id)
                        
                        // Swipe to Queue Action
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.addToNext(listOf(song))
                                    return@rememberSwipeToDismissBoxState false // Don't dismiss, just trigger action
                                }
                                false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) com.vagueplayer.music.ui.theme.AccentBlue else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 12.dp), // [RESIZE] Reduced from 20dp
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.PlaylistPlay, 
                                            contentDescription = "Play Next", 
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            content = {
                                // Flat List Style
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White) // Ensure opaque background for swipe
                                        .combinedClickable(
                                            onClick = { 
                                                if (isSelectionMode) {
                                                    viewModel.toggleSelection(song.id)
                                                } else {
                                                    viewModel.playSong(song, songs, "所有歌曲") 
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    viewModel.setSelectionMode(true)
                                                    viewModel.toggleSelection(song.id)
                                                }
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 8.dp), // [RESIZE] Reduced from 16dp to 8dp 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Album Art
                                    AsyncImage(
                                        model = song.albumArtUri,
                                        contentDescription = "Album Art",
                                        modifier = Modifier
                                            .size(56.dp) 
                                            .clip(RoundedCornerShape(8.dp)) 
                                            .background(Color.Gray.copy(alpha = 0.3f)),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = android.R.drawable.ic_menu_crop) 
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected && isSelectionMode) com.vagueplayer.music.ui.theme.AccentBlue else Color.Black,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 14.sp,
                                            color = Color.Gray, 
                                            maxLines = 1
                                        )
                                    }

                                    // Checkbox Only (Menu Removed)
                                    if (isSelectionMode) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.toggleSelection(song.id) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = com.vagueplayer.music.ui.theme.AccentBlue,
                                                unselectedColor = Color.Gray
                                            )
                                        )
                                    }
                                }
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
                if (songs.isNotEmpty()) {
                    com.vagueplayer.music.ui.components.AlphabetSideBar(
                        sections = sidebarSections,
                        onLetterSelected = { char ->
                            // Logic: Find first song starting with this char OR greater
                            val targetIndex = if (char == '#') {
                                0 // Top
                            } else {
                                // Find first song where the Pinyin Index matches the selected char OR is greater
                                // "Greater" is needed if the exact letter doesn't exist (e.g. click 'Q', go to 'R')
                                songs.indexOfFirst { song ->
                                    val indexLetter = com.vagueplayer.music.utils.PinyinUtils.getIndexLetter(song.title)
                                    if (indexLetter == '#') false // Skip symbols when looking for letters
                                    else if (indexLetter in 'A'..'Z') indexLetter >= char
                                    else false
                                }
                            }
                            
                            if (targetIndex != -1) {
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(if (isSidebarLeft) Alignment.TopStart else Alignment.TopEnd) // visual Up
                            .padding(top = 80.dp, bottom = 0.dp) // Align EXACTLY with Header Top (80dp)
                            .heightIn(max = 600.dp) // Maximize height for all letters
                    )
                }
            } // Close Inner Box (List + Sidebar)
        }
    } // Close Outer Box (PullRefresh)
    
    // OVERLAY LAYER - Real Glass Dropdown (Custom Implementation)
    // OVERLAY LAYER - Morphing Glass Menu
    
    // OVERLAY LAYER - Dialog Removed (Replaced by Dropdown)

    // OVERLAY LAYER - Dialog Removed (Replaced by Dropdown)

    PullRefreshIndicator(
        refreshing = isScanning,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = com.vagueplayer.music.ui.theme.AccentBlue
        )
    }
}
