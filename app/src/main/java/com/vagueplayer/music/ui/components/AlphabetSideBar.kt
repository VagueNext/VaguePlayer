package com.vagueplayer.music.ui.components // Checking correct package

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max

@Composable
fun AlphabetSideBar(
    sections: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    // -1f means no active touch
    var touchedIndex by remember { mutableStateOf(-1f) }
    var lastSelectedIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = modifier
            .width(20.dp) // Slightly wider to allow scaling expansion without clipping if needed (visual only)
            .padding(vertical = 4.dp)
            .pointerInput(sections) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // Calculate index based on total height
                        val index = ((offset.y / size.height) * sections.size).coerceIn(0f, sections.lastIndex.toFloat())
                        touchedIndex = index
                        
                        val i = index.toInt()
                        if (i != lastSelectedIndex && i in sections.indices) {
                            lastSelectedIndex = i
                            onLetterSelected(sections[i])
                        }
                    },
                    onDragEnd = {
                         touchedIndex = -1f
                         lastSelectedIndex = -1
                    },
                    onDragCancel = {
                         touchedIndex = -1f
                         lastSelectedIndex = -1
                    },
                    onVerticalDrag = { change, _ ->
                        val index = ((change.position.y / size.height) * sections.size)
                            // Allow slightly out of bounds dragging to keep selecting ends
                            .coerceIn(-0.5f, sections.size - 0.5f) 
                        
                        touchedIndex = index

                        val i = index.toInt().coerceIn(0, sections.lastIndex)
                        if (i != lastSelectedIndex) {
                            lastSelectedIndex = i
                            onLetterSelected(sections[i])
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(0.dp), // Maximum tightness
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        sections.forEachIndexed { index, letter ->
            // Animation State Calculation
            val isTouched = touchedIndex != -1f
            
            // Calculate distance from touch center
            val distance = if (isTouched) abs(touchedIndex - index) else 0f
            
            // Non-linear scaling curve
            // Range: Effect affects about 2-3 items up and down
            val range = 2.5f 
            
            // Target Scale
            // If distance < range, scale up. 
            // Using a cubic falloff for "non-linear" sharpness
            val sizeScale = if (distance < range && isTouched) {
                val progress = 1f - (distance / range)
                1f + 1.5f * (progress * progress * progress) // Max scale 2.5x
            } else {
                1f
            }
            
            // Animate the scale using Unified Animation System
            val animatedScale by animateFloatAsState(
                targetValue = sizeScale,
                animationSpec = com.vagueplayer.music.ui.animation.AnimationSpecs.GlassDeformation.Scale, 
                label = "scale"
            )
            
            // Dynamic translation to push neighbors slightly away (optional, simplified to just scale for now)
            
            // Visuals
            val isSelected = index == lastSelectedIndex
            
            Text(
                text = letter.toString(),
                fontSize = 10.sp, 
                lineHeight = 10.sp,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false 
                    )
                ),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) com.vagueplayer.music.ui.theme.AccentBlue else Color.Gray,
                modifier = Modifier
                    .padding(vertical = 0.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        // Pivot on the Right edge so it grows Leftwards into the screen
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        
                        // Optional: Fade out distant letters slightly?
                        alpha = if (isTouched) {
                             // Keep near letters opaque, fade distant ones
                             1f - (distance / 10f).coerceIn(0f, 0.5f)
                        } else 1f
                    }
            )
        }
    }
}
