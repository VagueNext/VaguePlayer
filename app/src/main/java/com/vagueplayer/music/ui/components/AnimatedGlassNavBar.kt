package com.vagueplayer.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun AnimatedGlassNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    collapseProgress: Float = 0f,
    expandedWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // [LOGIC] Gradual Cover Animation
    // 1. Force the content Row to stay at 'Expanded Width' even when container shrinks.
    // 2. Translate the Row horizontally so the 'Selected Icon' aligns with the center of the container.
    // 3. Container automatically clips the overflow (padding logic in parent handles frame).
    
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // Center content in Pill
        val density = LocalDensity.current
        
        // Use fixed width for calculations to prevent squishing
        val totalWidthPx = with(density) { expandedWidth.toPx() }
        val itemWidthPx = totalWidthPx / items.size
        
        // Calculate Translation to center the selected item
        // Target: Center of Container (which is shrinking, but we are aligned to it)
        // Row Center: totalWidthPx / 2
        // Selected Item Center: (Index + 0.5) * itemWidthPx
        // Shift needed relative to Row Center: RowCenter - ItemCenter
        
        val rowCenter = totalWidthPx / 2f
        val itemCenter = (selectedIndex + 0.5f) * itemWidthPx
        val targetTranslation = (rowCenter - itemCenter)
        
        // Interpolate: 0f (Normal Layout) -> targetTranslation (Centered Mode)
        val currentTranslation by animateFloatAsState(
            targetValue = targetTranslation * collapseProgress,
            label = "NavTranslation"
        )

        // 1. Animated Glass Indicator (Pill)
        val indicatorOffset by animateIntOffsetAsState(
            targetValue = IntOffset(
                x = (itemWidthPx * selectedIndex).toInt(),
                y = 0
            ),
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.75f, 
                stiffness = 300f 
            ),
            label = "IndicatorOffset"
        )
        
        // Render Indicator only if not fully collapsed
        // It moves WITH the Row, so its offset is relative to the Row's origin.
        if (collapseProgress < 0.8f) { // Fade out earlier
            val indicatorAlpha = (1f - collapseProgress * 2).coerceIn(0f, 1f)
            val isSelectedPressed by remember(items) { items.map { MutableInteractionSource() } }[selectedIndex].collectIsPressedAsState() // Re-access source later
            
            Box(
                modifier = Modifier
                    .width(with(density) { itemWidthPx.toDp() })
                    .fillMaxHeight()
                    .graphicsLayer { 
                        translationX = currentTranslation // Move with Row? No, Indicator is usually separate layer.
                        // Wait, if Row moves, Indicator must move matching the icons.
                        // Ideally Indicator should be INSIDE the shifted container or shifted same way.
                        // Let's apply translation to a common parent of Icons and Indicator?
                        // YES.
                    }
            )
            // Wait, existing code had Indicator separate from Row.
            // Let's create a wrapper Box that applies the translation to BOTH.
        }

        // [WRAPPER] Holds fixed-width content and translates it
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    // [FORCE] Ignore parent constraints (which are shrinking)
                    // Measure strictly at expandedWidth
                    val forcedWidth = totalWidthPx.toInt()
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = forcedWidth,
                            maxWidth = forcedWidth
                        )
                    )
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = currentTranslation
                }
        ) {
            val interactionSources = remember(items) { items.map { MutableInteractionSource() } }

            // 1. Indicator (Inside Wrapper)
             // [VISUAL FIX] Sync Indicator Scale with Icon Press
            val isSelectedPressed by interactionSources[selectedIndex].collectIsPressedAsState()
            
            val indicatorScale by animateFloatAsState(
                targetValue = if (isSelectedPressed) 0.85f else 1f, 
                animationSpec = if (isSelectedPressed) com.vagueplayer.music.ui.animation.AnimationSpecs.ElasticSnappy else com.vagueplayer.music.ui.animation.AnimationSpecs.ElasticJelly,
                label = "IndicatorScale"
            )
            
            if (collapseProgress < 0.9f) {
                 Box(
                    modifier = Modifier
                        .width(with(density) { itemWidthPx.toDp() })
                        .fillMaxHeight()
                        .offset { indicatorOffset } // Relative to Wrapper Left
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .graphicsLayer { 
                            scaleX = indicatorScale
                            scaleY = indicatorScale
                            alpha = (1f - collapseProgress * 3).coerceIn(0f, 1f)
                        }
                        // Add subtle border for better "Active" feedback
                        .border(
                            width = 0.5.dp, 
                            color = Color.White.copy(alpha = 0.2f), 
                            shape = RoundedCornerShape(100.dp)
                        )
                        .simpleGlass(
                            cornerRadius = 100.dp, 
                            tint = Color.White.copy(alpha = 0.15f),
                            distortionStrength = 20f,
                            edgeWidth = 15f
                        )
                )
            }

            // 2. Icons Row (Inside Wrapper)
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val iconColor = if (isSelected) com.vagueplayer.music.ui.theme.AccentBlue else Color.Gray

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            // Bouncy Click
                            .bouncyClickable(
                                interactionSource = interactionSources[index],
                                targetScale = 0.85f,
                                onClick = { onItemSelected(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
