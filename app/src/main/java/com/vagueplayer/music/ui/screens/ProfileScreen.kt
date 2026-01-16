package com.vagueplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vagueplayer.music.ui.theme.AccentBlue

import com.vagueplayer.music.ui.components.waterDropGlass
import com.vagueplayer.music.ui.components.LiquidGlassDefaults
import com.vagueplayer.music.ui.animation.transformSource // [NEW]
import androidx.compose.animation.ExperimentalSharedTransitionApi
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    hazeState: HazeState? = null,
    onNavigateToSettings: () -> Unit = {},
    onQuickAction: (String) -> Unit = {},
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // ... (Header logic unchanged, not shown here to save tokens if we can verify context)
        // Wait, replace tool replaces the RANGE. I need to be careful not to delete header.
        Text(
            text = "我的",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 80.dp, bottom = 24.dp)
        )

        // User Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
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

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionCard(
                "最近播放", 
                hazeState, 
                Modifier
                    .weight(1f)
                    .clickable { onQuickAction("recent") } 
            )
            QuickActionCard(
                "收藏歌曲", 
                hazeState, 
                Modifier
                    .weight(1f)
                    .clickable { onQuickAction("favorites") }
            )
            QuickActionCard(
                "已移除", 
                hazeState, 
                Modifier
                    .weight(1f)
                    .clickable { onQuickAction("removed") }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Settings Entry
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
                .clickable { onNavigateToSettings() } 
                .padding(horizontal = 16.dp)
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                         Modifier.transformSource("settings_card", sharedTransitionScope, animatedVisibilityScope)
                    } else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            // [NEW] Shared Element Source
            val sharedMod = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                     Modifier.transformSource("settings_card", this, animatedVisibilityScope)
                }
            } else Modifier
            
            Box(modifier = Modifier.fillMaxSize().then(sharedMod)) // Apply to internal box to match shape?
            // Actually usually transformSource goes on the SURFACE (the visible container).
            // The outer Box has the background.
            
            Text(
                "设置", 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Medium,
                modifier = Modifier.then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(key = "settings_text"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                renderInOverlayDuringTransition = false
                            )
                        }
                    } else Modifier
                )
            )
        }
    }
}

@Composable
fun QuickActionCard(title: String, hazeState: HazeState?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .waterDropGlass(
                hazeState = hazeState,
                blurRadius = LiquidGlassDefaults.BlurRadius,
                tint = LiquidGlassDefaults.Tint,
                edgeWidth = LiquidGlassDefaults.EdgeWidth,
                distortionStrength = LiquidGlassDefaults.DistortionStrength,
                cornerRadius = 16.dp,
                enableShader = true
            )
            .padding(16.dp)
    ) {
        Text(
            text = title, 
            modifier = Modifier.align(Alignment.BottomStart), 
            fontWeight = FontWeight.Bold
        )
    }
}
