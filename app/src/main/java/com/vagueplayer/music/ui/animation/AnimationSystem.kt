package com.vagueplayer.music.ui.animation

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * VaguePlayer 统一动画系统 (Unified Animation System)
 * 
 * 合并自: PhysicsConstants, NonLinearEasing, AnimationSpecs, GooeyMergeEffect, MorphTransition
 */

// =============================================================================
// 1. 物理常量 (Physics Constants)
// =============================================================================

object PhysicsConstants {
    // Spring Stiffness
    const val STIFFNESS_HIGH = Spring.StiffnessHigh // 10000f
    const val STIFFNESS_MEDIUM = Spring.StiffnessMedium // 1500f
    const val STIFFNESS_LOW = Spring.StiffnessLow // 200f
    const val STIFFNESS_VERY_LOW = Spring.StiffnessVeryLow // 50f
    
    // Custom Stiffness
    const val STIFFNESS_JELLY = 600f
    const val STIFFNESS_SNAP = 2500f
    
    // Spring Damping
    const val DAMPING_NO_BOUNCE = 1f
    const val DAMPING_LOW_BOUNCE = Spring.DampingRatioLowBouncy // 0.75f
    const val DAMPING_MEDIUM_BOUNCE = Spring.DampingRatioMediumBouncy // 0.5f
    const val DAMPING_HIGH_BOUNCE = Spring.DampingRatioHighBouncy // 0.2f
    
    // Custom Damping
    const val DAMPING_JELLY = 0.6f
    const val DAMPING_FLUID = 0.85f
    
    // Durations
    const val DURATION_FAST = 200
    const val DURATION_NORMAL = 350
    const val DURATION_SLOW = 500
}

// =============================================================================
// 2. 非线性缓动曲线 (Non-Linear Easing)
// =============================================================================

object NonLinearEasing {
    /** Apple Standard - 平滑自然 */
    val AppleDefault: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    
    /** Material 3 Emphasized - 快进慢出 */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    /** Emphasized Decelerate - 入场动画 */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
}

// =============================================================================
// 3. 动画规格 (Animation Specs)
// =============================================================================

object AnimationSpecs {
    
    // --- 弹性形变效果 (ElasticDeformation) ---
    
    /** 标准形变 - 适度回弹 */
    val ElasticStandard: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 500f)
    
    /** 快速形变 - 干脆利落 (优化: 降低刚度，更流畅) */
    val ElasticSnappy: SpringSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 600f)
    
    /** 果冻形变 - 液态回弹 (优化: 更柔和的回弹) */
    val ElasticJelly: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 380f)
    
    /** 重物形变 - 慢速惯性 */
    val ElasticHeavy: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 300f)
    
    // --- 位置/尺寸动画 ---
    
    val PositionSpring: SpringSpec<IntOffset> = spring(dampingRatio = 0.7f, stiffness = 700f)
    val SizeSpring: SpringSpec<IntSize> = spring(dampingRatio = 0.6f, stiffness = 500f)
    val DpSpring: SpringSpec<Dp> = spring(dampingRatio = 0.6f, stiffness = 500f)
    val ColorSpring: SpringSpec<Color> = spring(dampingRatio = 0.6f, stiffness = 400f)
    
    // --- 曲线动画 ---
    
    val AppleCurve: FiniteAnimationSpec<Float> = tween(350, easing = NonLinearEasing.AppleDefault)
    val EmphasizedCurve: FiniteAnimationSpec<Float> = tween(400, easing = NonLinearEasing.Emphasized)
    
    // --- 玻璃效果专用 ---
    
    object GlassDeformation {
        val Scale = ElasticJelly
        val Position = ElasticSnappy
        val EdgeWidth = ElasticStandard
        val Distortion = ElasticJelly
    }
}

// =============================================================================
// 4. 液态融合效果 (Liquid Merge Effect)
// =============================================================================

fun Modifier.liquidMerge(blurRadius: Float = 60f): Modifier {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val radiusDp = (blurRadius / 3f).dp
        return this
            .blur(radiusDp)
            .drawWithCache {
                val paint = Paint()
                val alphaMatrix = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 60f, -3000f
                ))
                paint.colorFilter = ColorFilter.colorMatrix(alphaMatrix)
                
                onDrawWithContent {
                    val canvas = drawContext.canvas
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                    drawContent()
                    canvas.restore()
                }
            }
    }
    return this
}

// =============================================================================
// 5. 形变布局 (Morph Layout)
// =============================================================================

@Composable
fun <T> MorphLayout(
    targetState: T,
    extractBounds: (T) -> Rect,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val transition = updateTransition(targetState, label = "MorphLayout")
    val bounds by transition.animateRect(
        transitionSpec = { tween(350, easing = NonLinearEasing.AppleDefault) },
        label = "Bounds"
    ) { state -> extractBounds(state) }

    Layout(
        content = { content(targetState) },
        modifier = modifier
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = bounds.width.roundToInt(),
                maxWidth = bounds.width.roundToInt(),
                minHeight = bounds.height.roundToInt(),
                maxHeight = bounds.height.roundToInt()
            )
        )
        layout(bounds.width.roundToInt(), bounds.height.roundToInt()) {
            placeable.place(bounds.left.roundToInt(), bounds.top.roundToInt())
        }
    }
}
