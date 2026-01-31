package com.vagueplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.ui.theme.AccentBlue
import com.vagueplayer.music.ui.components.LiquidSwitch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.vagueplayer.music.ui.components.bouncyClickable
import com.vagueplayer.music.ui.animation.transformSource
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onQuickAction: (String) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    hazeState: HazeState? = null
) {
    // Use effective state (from MainScreen or local fallback)
    val effectiveHazeState = hazeState ?: remember { HazeState() }
    
    // Scroll State
    val listState: LazyListState = rememberLazyListState()
    
    // Calculate Header Alpha
    val scrollAlpha = remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            val threshold = 50f
            val scrollY = firstVisibleItemIndex * 80f + firstVisibleItemScrollOffset
            (scrollY / threshold).coerceIn(0f, 1f)
        }
    }.value

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .haze(effectiveHazeState),
            contentPadding = PaddingValues(top = 52.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
            item {
                // User Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = "Vague User",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "查看个人资料",
                            fontSize = 16.sp,
                            color = AccentBlue
                        )
                    }
                }
            }

            item {
                // Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionCard(
                        "最近播放", 
                        Modifier
                            .weight(1f)
                            .bouncyClickable(targetScale = 0.95f) { onQuickAction("recent") }
                    )
                    QuickActionCard(
                        "收藏歌曲", 
                        Modifier
                            .weight(1f)
                            .bouncyClickable(targetScale = 0.95f) { onQuickAction("favorites") }
                    )
                    QuickActionCard(
                        "已移除", 
                        Modifier
                            .weight(1f)
                            .bouncyClickable(targetScale = 0.95f) { onQuickAction("removed") }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Settings Entry
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .bouncyClickable(targetScale = 0.98f) { onNavigateToSettings() }
                        .padding(horizontal = 16.dp)
                        .then(
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                Modifier.transformSource("settings_card", sharedTransitionScope, animatedVisibilityScope)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Shared Element Source
                    // Inner box validation removed
                    Box(modifier = Modifier.fillMaxSize())
                    
                    Text(
                        "设置", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.then(
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
            }
        }

        // Floating Header
        com.vagueplayer.music.ui.components.ScreenHeader(
            title = "我的",
            scrollAlpha = scrollAlpha,
            hazeState = effectiveHazeState,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
fun QuickActionCard(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray.copy(alpha = 0.2f)) // Simple glass fallback
            .padding(16.dp)
    ) {
        Text(
            text = title, 
            modifier = Modifier.align(Alignment.BottomStart), 
            fontWeight = FontWeight.Bold
        )
    }
}
