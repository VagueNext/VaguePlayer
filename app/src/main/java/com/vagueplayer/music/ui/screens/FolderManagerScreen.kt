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
import com.vagueplayer.music.ui.components.waterDropGlass
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.viewmodel.AudioViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun FolderManagerScreen(
    viewModel: AudioViewModel,
    onBack: () -> Unit,
    hazeState: HazeState? = null
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // consistent with Settings for now, or ensure MainScreen background shines through?
            // User requested Glass UI. Let's make it a full glass overlay over the wallpaper?
            // If MainScreen already has wallpaper, we can use a semi-transparent scrim or a blurred sheet.
            // Let's stick to "Settings Style" (White) for consistency with the Transition, OR upgrade both.
            // Given "Glass Tree/List" request, let's use a subtle gradient or dark glass if it fits the theme.
            // For safety and consistency with current "Settings", let's use White but with Glass Cards.
            // actually, user said "Continue existing Glass Morphism style", which usually means Dark/Transparent in this app.
            // Let's try a Dark Glass Sheet.
            .background(Color.Black.copy(alpha = 0.5f)) 
    ) {
        // Glass Sheet Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp) // Full screen
                // We could apply hazeChild here to blur everything behind
                .waterDropGlass(hazeState, cornerRadius = 0.dp, tint = Color.Black.copy(alpha = 0.4f))
        ) {
             Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        "音乐文件夹",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // 1. System Library Toggle - REMOVED

                // 2. Custom Folders Header + Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "自定义文件夹",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { launcher.launch(null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Add, "Add Folder", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Folder List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                        folder.fullPath, // Or URI if path is empty
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
                    
                    // Spacer for bottom nav
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
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
