package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jellyfin.mobile.presentation.splash.model.SplashPoster

/**
 * Backward compatibility alias for [CinematicBackdropLayer].
 * Refactored to eliminate distorted 100.dp cropping and RTL gradient inversions.
 */
@Composable
fun CharacterSideArt(
    posters: List<SplashPoster>,
    modifier: Modifier = Modifier
) {
    CinematicBackdropLayer(
        posters = posters,
        modifier = modifier
    )
}
