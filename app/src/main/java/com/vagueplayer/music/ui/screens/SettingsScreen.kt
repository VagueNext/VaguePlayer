package com.vagueplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.ui.components.GlassDialog
import com.vagueplayer.music.ui.theme.AccentBlue
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.zIndex
// OverlayClip import removed
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze


@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: com.vagueplayer.music.viewmodel.AudioViewModel,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    // ... (States omitted for brevity, keeping original logic)
    // State
    var showFolderDialog by remember { mutableStateOf(false) }
    
    // Collect Preferences
    val isMixAudioEnabled by viewModel.isMixAudioEnabled.collectAsState()
    
    // Predictive Back State
    val swipeProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    var isBackGestureActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Predictive Back Handler
    androidx.activity.compose.PredictiveBackHandler { progress ->
        try {
            isBackGestureActive = true
            progress.collect { event ->
                swipeProgress.snapTo(event.progress)
            }
            // On Commit
            onBack()
        } catch (e: java.util.concurrent.CancellationException) {
            // On Cancel
            isBackGestureActive = false
            swipeProgress.animateTo(0f)
        }
    }

    // Animation Values (Derived from swipeProgress)
    val scale = 1f - (swipeProgress.value * 0.15f) // Shrink more during gesture
    val cornerRadius = 24.dp * swipeProgress.value // Larger corner radius
    val alpha = 1f - (swipeProgress.value * 0.7f) // Stronger fade to reveal background (0.3 at max)
    val translationX = swipeProgress.value * 200f // Slide right during back gesture

    // Scroll State
    val listState: LazyListState = rememberLazyListState()
    val scrollAlpha = remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            val threshold = 50f
            val scrollY = firstVisibleItemIndex * 80f + firstVisibleItemScrollOffset
            (scrollY / threshold).coerceIn(0f, 1f)
        }
    }.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { 
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    this.translationX = translationX
                    clip = true
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
                }
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "settings_card"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.ScaleToBounds(),
                                // clipInOverlayDuringTransition removed due to API mismatch
                                enter = androidx.compose.animation.EnterTransition.None,
                                exit = androidx.compose.animation.ExitTransition.None
                            )
                        }
                    } else Modifier
                )
                .background(Color.White)
        ) {
            LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 120.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
                item {
                    SettingsGroup("播放") {
                        SettingsSwitchItem(
                            title = "允许与其他应用同时播放",
                            subtitle = "不独占音频焦点 (混合播放)",
                            checked = isMixAudioEnabled,
                            onCheckedChange = { viewModel.setMixAudioEnabled(it) }
                        )
                        // REMOVED: Gapless switch (ExoPlayer handles gapless by default)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
            
                    SettingsGroup("媒体库") {
                        SettingsItem(
                            title = "管理音乐文件夹",
                            subtitle = "自定义扫描路径",
                            onClick = { showFolderDialog = true }
                        )

                        SettingsItem(
                            title = "重新扫描媒体库",
                            subtitle = "更新歌曲变动",
                            onClick = { viewModel.scanMedia() }
                        )
                        
                        val isSidebarLeft = viewModel.isSidebarOnLeft.collectAsState().value
                        SettingsSwitchItem(
                            title = "侧边索引栏位置",
                            subtitle = if(isSidebarLeft) "左侧 (Left)" else "右侧 (Right)",
                            checked = isSidebarLeft,
                            onCheckedChange = { viewModel.setSidebarOnLeft(it) },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SettingsGroup("关于") {
                        SettingsItem(
                            title = "版本",
                            subtitle = "1.0.0 (Native)",
                            showArrow = false
                        )
                    }
                }
            }

            // Floating Header
            com.vagueplayer.music.ui.components.ScreenHeader(
                title = "设置",
                scrollAlpha = scrollAlpha,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.TopCenter),
                navigationIcon = {
                    com.vagueplayer.music.ui.components.GlassIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp), // Use Modifier.size
                        glassTint = Color.Black.copy(alpha = 0.05f)
                    )
                }
            )
        }


        if (showFolderDialog) {
            FolderManagerDialog(
                viewModel = viewModel,
                onDismiss = { showFolderDialog = false }
            )
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color.Black)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        if (showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                "Go",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    // Divider logic could be added here for internal items
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState() // Not sufficient, need to know if animating?
    // Actually, we can use the 'checked' state change to trigger Z-index boost?
    // Or just "isPressed". User clicks -> Pressed -> ZIndex Up -> Animates.
    // But animation continues after press release.
    // Better: Lift Z-index if pressed OR recently toggled?
    // Let's keep it simple: Lift if interacting. 
    // And for "Visual Glitch" described by user, it's likely they see it during the toggle animation.
    
    // To ensure animation isn't clipped, we need to hold Z-index HIGH while animating.
    // LiquidSwitch animates logic internally.
    // We can infer animation state if we knew it.
    // Hack: Launch effect when 'checked' changes, hold High Z for 500ms.
    var isAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        isAnimating = true
        kotlinx.coroutines.delay(600) // Slightly longer than spring
        isAnimating = false
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isAnimating || isPressed) 10f else 0f)
            .graphicsLayer { clip = false }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) { onCheckedChange(!checked) } 
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color.Black)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        // Use custom LiquidSwitch
        com.vagueplayer.music.ui.components.LiquidSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            checkedTrackColor = com.vagueplayer.music.ui.theme.AccentBlue,
            interactionSource = interactionSource // Share source
        )
    }
}

@Composable
private fun FolderManagerDialog(
    viewModel: com.vagueplayer.music.viewmodel.AudioViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val customFolders by viewModel.customFolders.collectAsState()
    
    // Folder Picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
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

    GlassDialog(
        title = "音乐文件夹",
        description = "管理扫描路径",
        icon = Icons.Default.Usb, 
        onDismissRequest = onDismiss,
        confirmText = "添加文件夹",
        onConfirm = { launcher.launch(null) },
        cancelText = "关闭",
        onCancel = onDismiss,
        content = {
            // Folder List
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp) 
            ) {
                 if (customFolders.isEmpty()) {
                     Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                         Text("暂无文件夹", color = Color.Gray)
                     }
                 } else {
                     LazyColumn(
                         verticalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         items(customFolders.size) { index ->
                             val folder = customFolders[index]
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clip(RoundedCornerShape(8.dp))
                                     .background(Color.Black.copy(alpha = 0.05f))
                                     .padding(12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(
                                     Icons.Default.Usb, 
                                     null, 
                                     tint = AccentBlue, 
                                     modifier = Modifier.size(20.dp)
                                 )
                                 Spacer(modifier = Modifier.width(12.dp))
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(folder.displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                     Text(folder.fullPath, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                 }
                                 IconButton(
                                     onClick = { viewModel.removeCustomFolder(folder.uri) },
                                     modifier = Modifier.size(24.dp)
                                 ) {
                                     Icon(
                                         imageVector = Icons.Default.Close, 
                                         contentDescription = "Remove",
                                         tint = Color.Gray
                                     )
                                 }
                             }
                         }
                     }
                 }
            }
        }
    )
}
