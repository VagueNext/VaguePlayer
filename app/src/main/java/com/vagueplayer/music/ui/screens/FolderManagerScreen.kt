package com.vagueplayer.music.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.viewmodel.AudioViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun FolderManagerScreen(
    viewModel: AudioViewModel,
    onBack: () -> Unit,
    hazeState: HazeState? = null
) {
    // Use LOCAL HazeState
    val localHazeState = remember { HazeState() }
    
    val context = LocalContext.current
    val customFolders by viewModel.customFolders.collectAsState()

    // Folder Picker Launcher
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            // Persist permission (important for long-term access)
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

    // Scroll State
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollAlpha = remember {
        derivedStateOf {
            val firstVisibleItemIndex = scrollState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = scrollState.firstVisibleItemScrollOffset
            val scrollY = firstVisibleItemIndex * 100f + firstVisibleItemScrollOffset
            (scrollY / 50f).coerceIn(0f, 1f)
        }
    }.value

    // Changed Column to Box to allow Floating Header overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) 
    ) {
        // CONTENT LAYER
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .haze(localHazeState), // Mark as Haze Source
            contentPadding = PaddingValues(top = 66.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "自定义文件夹",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(customFolders) { folder ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                folder.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                folder.fullPath,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { viewModel.removeCustomFolder(folder.uri) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            
            if (customFolders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "没有自定义文件夹",
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Floating Header
        com.vagueplayer.music.ui.components.ScreenHeader(
            title = "音乐文件夹",
            scrollAlpha = scrollAlpha,
            hazeState = localHazeState, // Use local HazeState
            contentColor = Color.White,
            glassTint = Color.Black.copy(alpha = 0.5f),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            },
            action = {
                IconButton(
                    onClick = { launcher.launch(null) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Add, "Add Folder", tint = Color.White)
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter) // Updated for Box layout
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f)) // Simple translucent background for list items
            // If we want heavy glass on each item, we could use waterDropGlass here too, but it might be heavy.
            // Let's stick to a clean translucent "Glass Material" look.
    ) {
        content()
    }
}
