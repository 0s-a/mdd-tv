package org.jellyfin.mobile.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * FloatingFrostedTopBar provides a detached, floating glassmorphic top bar.
 * Matches FloatingBottomNavigationBar exactly:
 * - Floating capsule with 70% opacity and subtle 1dp cyan glass outline.
 * - Detached with horizontal margins (non-continuous / بشكل غير متصل).
 * - Fixed height and statusBarsPadding for edge-to-edge Android 15.
 */
@Composable
fun FloatingFrostedTopBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassmorphism(
                    backgroundColor = JellyfinColors.SurfaceGlass,
                    borderColor = JellyfinColors.BorderGlass,
                    cornerRadius = JellyfinDimens.RadiusPill
                )
                .padding(
                    horizontal = JellyfinDimens.SpaceMedium,
                    vertical = JellyfinDimens.SpaceExtraSmall
                ),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
