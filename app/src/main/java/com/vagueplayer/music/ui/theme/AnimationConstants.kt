package com.vagueplayer.music.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith

object AnimationConstants {
    // Unified Spring Physics (Matches Player Expansion)
    const val ContainerStiffness = 350f
    const val ContainerDamping = 0.75f
    
    val ContainerSpringSpec = spring<Float>(
        dampingRatio = ContainerDamping,
        stiffness = ContainerStiffness
    )
    
    // Unified Container Transform (Expand from center/bottom with fade)
    val ContainerTransformSpec = {
        (fadeIn(tween(300)) + 
         scaleIn(initialScale = 0.92f, animationSpec = ContainerSpringSpec))
         .togetherWith(
            fadeOut(tween(200)) + 
            scaleOut(targetScale = 0.92f, animationSpec = ContainerSpringSpec)
        )
    }
}
