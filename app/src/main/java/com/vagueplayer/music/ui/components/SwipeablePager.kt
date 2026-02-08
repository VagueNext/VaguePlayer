package com.vagueplayer.music.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vagueplayer.music.ui.animation.PhysicsConstants.DAMPING_JELLY
import com.vagueplayer.music.ui.animation.PhysicsConstants.STIFFNESS_JELLY

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeablePager(
    pageCount: Int,
    selectedIndex: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = selectedIndex) { pageCount }

    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    // [FIX] First Frame Guard to prevent reset to 0
    // [FIX] First Frame Guard to prevent reset to 0
    // Use 'remember' instead of 'rememberSaveable' so it RESETS on process recreation
    // This ensures we re-run the 0-check when the app is restored from background.
    var isInitialized by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedIndex) {
            
            // [FIX] Strict Guard Logic for State Mismatch
            // If Pager reports 0 during initialization while we expect >0, assume it's a stale/default state.
            if (!isInitialized && selectedIndex > 0 && pagerState.currentPage == 0) {
                 android.util.Log.w("NavDebug", "Detected State Mismatch. Selected=$selectedIndex, Pager=0. Correcting...")
                 
                 // [FIX] FORCE Correct visual state immediately (No Animation)
                 pagerState.scrollToPage(selectedIndex)
                 
                 // Mark as initialized only AFTER we force correction
                 isInitialized = true
                 return@LaunchedEffect
            }
            
            // Only update external state if initialized
            if (isInitialized) {
                onPageChanged(pagerState.currentPage)
            } else {
                // First valid settle event
                isInitialized = true
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            // Custom snap animation spec
            snapAnimationSpec = spring(
                dampingRatio = DAMPING_JELLY,
                stiffness = STIFFNESS_JELLY
            )
        )
    ) { page ->
        content(page)
    }
}
