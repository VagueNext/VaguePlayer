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

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedIndex) {
            onPageChanged(pagerState.currentPage)
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
