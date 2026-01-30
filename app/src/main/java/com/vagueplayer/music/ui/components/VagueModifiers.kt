package com.vagueplayer.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.vagueplayer.music.ui.animation.AnimationSpecs

/**
 * VaguePlayer 统一交互修饰符 (Unified Interaction Modifiers)
 * 
 * 使用 AnimationSpecs 弹性形变系统
 */

/**
 * 弹性点击效果 (Bouncy Click Effect)
 * 
 * 使用统一的 ElasticDeformation 物理:
 * - 按下: ElasticSnappy (快速响应)
 * - 释放: ElasticJelly (液态回弹)
 * 
 * @param targetScale 按下时的缩放比例, 默认 1.05f
 * @param onClick 点击回调
 * @param onLongClick 长按回调 (可选)
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyClickable(
    targetScale: Float = 1.05f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    bouncyClickable(
        interactionSource = remember { MutableInteractionSource() },
        targetScale = targetScale,
        onLongClick = onLongClick,
        onClick = onClick
    )
}

/**
 * Overload allowing external InteractionSource hoisting
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyClickable(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 1.05f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 使用统一的弹性形变系统
    val animationSpec = if (isPressed) {
        AnimationSpecs.ElasticSnappy  // 按下: 快速响应
    } else {
        AnimationSpecs.ElasticJelly   // 释放: 液态回弹
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = animationSpec,
        label = "Bouncy Touch Scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null, // 无涟漪, 仅弹性
            onClick = onClick,
            onLongClick = onLongClick
        )
}
