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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.graphicsLayer // [FIX] Added import
import androidx.compose.animation.core.Animatable // [FIX] Added import
import androidx.compose.runtime.rememberCoroutineScope // [FIX] Added import
import androidx.compose.material.icons.filled.Close // [FIX] Restored import
import dev.chrisbanes.haze.haze // [FIX] Added haze import


@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    viewModel: com.vagueplayer.music.viewmodel.AudioViewModel,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    // ... (States omitted for brevity, keeping original logic)
    // State
    var showFolderDialog by remember { mutableStateOf(false) }
    
    // Collect Preferences
    val isMixAudioEnabled by viewModel.isMixAudioEnabled.collectAsState()
    
    // Local Haze State for Settings (Fixes Ghosting)
    val settingsHazeState = remember { dev.chrisbanes.haze.HazeState() }
    
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
    val scale = 1f - (swipeProgress.value * 0.1f)
    val cornerRadius = 16.dp * swipeProgress.value
    // Optional: Slide feedback
    // val translationY = 100.dp * swipeProgress.value 

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // [FIX] Transparent background to reveal underlying content
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Apply Predictive Back Transforms
                .graphicsLayer { // [FIX] Correct syntax
                    scaleX = scale
                    scaleY = scale
                    clip = true
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
                    // translationY = translationY.toPx() 
                }
                .background(Color.White)
                // [FIX] Apply Haze Source HERE, so dialogs blur THIS content
                .haze(settingsHazeState) // [FIX] Correct syntax
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
                }
                Text(
                    "设置",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 8.dp)
                        .then(
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "settings_text"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.ScaleToBounds(),
                                        enter = androidx.compose.animation.EnterTransition.None,
                                        exit = androidx.compose.animation.ExitTransition.None
                                    )
                                }
                            } else Modifier
                        )
                )
            }

            // Settings Groups
            SettingsGroup("播放") {
                SettingsSwitchItem(
                    title = "允许与其他应用同时播放",
                    subtitle = "不独占音频焦点 (混合播放)",
                    checked = isMixAudioEnabled,
                    onCheckedChange = { viewModel.setMixAudioEnabled(it) },
                    hazeState = settingsHazeState // [FIX] Use Local Haze
                )
                
                val isGaplessEnabled by viewModel.isGaplessEnabled.collectAsState()
                SettingsSwitchItem(
                    title = "无缝播放",
                    subtitle = "自动跳过首尾静音片段 切歌时声音不间断",
                    checked = isGaplessEnabled,
                    onCheckedChange = { viewModel.setGaplessEnabled(it) },
                    hazeState = settingsHazeState // [FIX] Use Local Haze
                )
            }

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
                // Sidebar Position Toggle
                val isSidebarLeft = viewModel.isSidebarOnLeft.collectAsState().value
                SettingsSwitchItem(
                    title = "侧边索引栏位置",
                    subtitle = if(isSidebarLeft) "左侧 (Left)" else "右侧 (Right)",
                    checked = isSidebarLeft,
                    onCheckedChange = { viewModel.setSidebarOnLeft(it) },
                    hazeState = settingsHazeState // [FIX] Use Local Haze
                )
            }
    
            Spacer(modifier = Modifier.height(24.dp))
    
            // SettingsGroup("外观") { ... } REMOVED
            
            Spacer(modifier = Modifier.weight(1f))
            
            SettingsGroup("关于") {
                SettingsItem(
                    title = "版本",
                    subtitle = "1.0.0 (Native)",
                    showArrow = false
                )
            }
        }

        if (showFolderDialog) {
            FolderManagerDialog(
                viewModel = viewModel,
                hazeState = settingsHazeState, // [FIX] Use Local Haze
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
    hazeState: dev.chrisbanes.haze.HazeState? = null // Add HazeState parameter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            hazeState = hazeState,
            checkedTrackColor = com.vagueplayer.music.ui.theme.AccentBlue
        )
    }
}

@Composable
private fun FolderManagerDialog(
    viewModel: com.vagueplayer.music.viewmodel.AudioViewModel,
    hazeState: dev.chrisbanes.haze.HazeState?,
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
        hazeState = hazeState,
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
