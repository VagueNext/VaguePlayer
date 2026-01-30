package com.vagueplayer.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * 字母索引栏组件 (Alphabet Sidebar)
 * 用于快速滚动音乐库列表
 * Enhanced with touch drag, position awareness, and floating bubble hint.
 */
@Composable
fun AlphabetSideBar(
    sections: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
    isOnLeft: Boolean = false // Position: left or right
) {
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var barHeight by remember { mutableStateOf(0) }
    
    // 动画缩放
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "index_scale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .zIndex(100f)
    ) {
        // 字母索引列表
        Column(
            modifier = Modifier
                .wrapContentHeight() // [FIX] Wrap height to content (fixes wide spacing)
                .width(20.dp) // [FIX] Fixed width for touch target
                .align(if (isOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(
                    start = if (isOnLeft) 4.dp else 0.dp,
                    end = if (isOnLeft) 0.dp else 4.dp
                )
                .scale(scale)
                .onGloballyPositioned { coordinates ->
                    barHeight = coordinates.size.height
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            selectedLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            selectedLetter = null
                        }
                    ) { change, _ ->
                        change.consume()
                        val y = change.position.y
                        if (barHeight > 0) {
                            val index = ((y / barHeight) * sections.size)
                                .toInt()
                                .coerceIn(0, sections.lastIndex)
                            val letter = sections[index]
                            if (selectedLetter != letter) {
                                selectedLetter = letter
                                onLetterSelected(letter)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (barHeight > 0) {
                            val index = ((offset.y / barHeight) * sections.size)
                                .toInt()
                                .coerceIn(0, sections.lastIndex)
                            val letter = sections[index]
                            selectedLetter = letter
                            onLetterSelected(letter)
                        }
                    }
                }
                .background(Color.Transparent), // Touch target
            horizontalAlignment = Alignment.CenterHorizontally,
            // verticalArrangement = Arrangement.SpaceEvenly // Removed as per instruction
        ) {
            sections.forEach { letter ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp), // Fixed height per letter
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        fontSize = 10.sp,
                        fontWeight = if (selectedLetter == letter) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedLetter == letter) com.vagueplayer.music.ui.theme.AccentBlue else Color.Gray,
                        modifier = Modifier.alpha(if (selectedLetter == letter) 1f else 0.6f)
                    )
                }
            }
        }

        // 浮动提示
        AnimatedVisibility(
            visible = selectedLetter != null && isDragging,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = if (isOnLeft) 60.dp else (-60).dp),
            enter = scaleIn(spring(stiffness = 500f)) + fadeIn(),
            exit = scaleOut(spring(stiffness = 500f)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(com.vagueplayer.music.ui.theme.AccentBlue.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter?.toString() ?: "",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
