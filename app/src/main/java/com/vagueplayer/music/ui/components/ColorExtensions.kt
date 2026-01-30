
package com.vagueplayer.music.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// 判断颜色是否属于“亮色”
fun Color.isLight(): Boolean {
    // 使用 W3C 标准 luminance 算法
    return this.luminance() > 0.5f
}

// 获取最佳对比度的内容颜色（字色）
fun Color.contentColor(): Color {
    return if (this.isLight()) {
        Color.Black.copy(alpha = 0.87f) // 浅色背景用深色字
    } else {
        Color.White // 深色背景用白色字
    }
}
