package com.vagueplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // [FIX] Import Edit Icon
import androidx.compose.foundation.lazy.itemsIndexed // [FIX] Import itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.* // [FIX] expanded to * for remember, mutableState
import androidx.activity.compose.PredictiveBackHandler // [FIX] Import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vagueplayer.music.data.model.Playlist
import com.vagueplayer.music.viewmodel.AudioViewModel
import kotlinx.coroutines.flow.collect // [FIX] Import for flow collection

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: AudioViewModel, // Retained signature for potential future use (e.g. playing songs)
    onDismissRequest: () -> Unit, // Renamed from onBack to match MainScreen call site? User snippet used onBack. MainScreen uses onDismissRequest. I'll use onDismissRequest.
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Predictive Back State
    var backProgress by remember { mutableFloatStateOf(0f) }
    val scale = 1f - (backProgress * 0.1f) // Scale down to 90%
    val alpha = 1f - (backProgress * 0.2f) // Fade out slightly
    val yOffset = backProgress * 100f // Slide down slightly

    // Handle Predictive Back
    androidx.activity.compose.PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            // On Commit
            onDismissRequest()
        } catch (e: java.util.concurrent.CancellationException) {
            // On Cancel - ensure it snaps back (Compose state will handle this naturally when progress stops emitting, 
            // but we reset manually to be safe if the flow cancellation leaves artifacts)
            backProgress = 0f
        } finally {
             backProgress = 0f
        }
    }

    // 1. Container Target (sharedBounds)
    // State for Rename Dialog
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name) }

    // 1. Container Target (sharedBounds)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            // Apply Predictive Back Transform
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                translationY = yOffset
            }
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "container_${playlist.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) },
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ),
        color = MaterialTheme.colorScheme.background // Or darker color to contrast with white sheet?
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 2. Image Header (Fixed at top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Reduced height slightly to give more space to list
            ) {
                val coverUri = playlist.songs.firstOrNull()?.albumArtUri
                
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUri)
                        .crossfade(false)
                        .placeholderMemoryCacheKey("cover_${playlist.id}")
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            state = rememberSharedContentState(key = "image_${playlist.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) }
                        )
                )

                // Scrim for status bar / back button visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )

                // Back Button (Top Left)
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                        // [FIX] Removed graphicsLayer shadow which caused the "Box" effect
                    )
                }
            }

            // 3. White Sheet Content (List)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Fill remaining space
                color = Color.White,
                // shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // [USER REQUEST] Removed rounded corners
                tonalElevation = 0.dp
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                ) {
                    // Title & Metadata Section
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
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
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "title_${playlist.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                                            enter = EnterTransition.None,
                                            exit = ExitTransition.None
                                        )
                                )
                                // [FIX] Removed Edit Icon
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${playlist.songs.size} 首歌曲",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    // Songs List
                    itemsIndexed(playlist.songs) { index, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playSong(song, playlist.songs, playlist.name)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp), // [MATCH LIBRARY] 8dp padding
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             // [MATCH LIBRARY] Album Art
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

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 17.sp, // [MATCH LIBRARY] 17sp
                                    fontWeight = FontWeight.Medium, // [MATCH LIBRARY] Medium
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 14.sp, // [MATCH LIBRARY] 14sp
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Rename Dialog
    if (showRenameDialog) {
        com.vagueplayer.music.ui.components.GlassDialog(
            hazeState = null, // Detail screen doesn't have HazeState handy usually, pass null or hoist
            title = "重命名歌单",
            description = "请输入新的歌单名称",
            icon = androidx.compose.material.icons.Icons.Default.Edit,
            onDismissRequest = { showRenameDialog = false },
            confirmText = "保存",
            onConfirm = {
                if (renameText.isNotBlank()) {
                    viewModel.renamePlaylist(playlist.id, renameText)
                    showRenameDialog = false
                }
            },
            cancelText = "取消",
            onCancel = { showRenameDialog = false },
            content = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.vagueplayer.music.ui.theme.AccentBlue,
                        cursorColor = com.vagueplayer.music.ui.theme.AccentBlue
                    )
                )
            }
        )
    }
}
