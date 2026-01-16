package com.vagueplayer.music.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

object AnimationUtils {

    // === Physics Constants ===
    // "Apple-like" fluid spring physics
    const val SPRING_DAMPING = 0.8f
    const val SPRING_STIFFNESS = 380f
    
    // Standard durations
    const val DURATION_SHORT = 300
    const val DURATION_MEDIUM = 400

    // === Animation Specs ===
    
    // The core spring spec for offset/size animations
    fun <T> appleSpring() = spring<T>(
        dampingRatio = SPRING_DAMPING,
        stiffness = SPRING_STIFFNESS
    )

    // Standard Fade spec
    fun standardFadeSpec() = tween<Float>(durationMillis = DURATION_SHORT)

    // === Helpers for Shared Element ===
    // Used in boundsTransform
    val sharedElementSpring = spring<Rect>(
        dampingRatio = SPRING_DAMPING,
        stiffness = SPRING_STIFFNESS
    )

    // === Transition Definitions ===

    /**
     * Standard Pop-up Enter Transition (e.g. for Settings, Dialogs)
     * Scales up from 92% to 100% with generic spring physics + Fade In.
     */
    val standardEnter: EnterTransition = 
        scaleIn(
            initialScale = 0.92f,
            animationSpec = appleSpring()
        ) + fadeIn(
            animationSpec = standardFadeSpec()
        )

    /**
     * Standard Pop-up Exit Transition
     * Scales down to 92% + Fade Out.
     */
    val standardExit: ExitTransition = 
        scaleOut(
            targetScale = 0.92f,
            animationSpec = appleSpring()
        ) + fadeOut(
            animationSpec = standardFadeSpec()
        )
}
