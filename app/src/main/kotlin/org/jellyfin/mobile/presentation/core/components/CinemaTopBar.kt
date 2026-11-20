package org.jellyfin.mobile.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Solid, unified Cinema Top Bar adhering to Refined Cinema & Obsidian Design System (Tokens 2.2.0).
 * Anti-AI Manifesto:
 * - NO floating detached capsules.
 * - NO cyan glass outlines.
 * - Solid OLED true black background (#000000) with subtle hairline bottom divider.
 * - Standard 56dp height and edge-to-edge statusBarsPadding.
 */
object CinemaTopBar {

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(JellyfinPrimitives.BlackOled)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = JellyfinDimens.SpaceLarge),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            HorizontalDivider(
                color = JellyfinColors.BorderHairline,
                thickness = JellyfinDimens.BorderHairline
            )
        }
    }

    /**
     * Root Cinema TopBar for HomeScreen:
     * - App title "مدد TV" on the right (RTL start)
     * - Immediate Search Action on the left (RTL end)
     * - Zero avatar distraction (Profile is now a primary bottom navigation tab).
     */
    @Composable
    fun Root(
        onSearchClick: () -> Unit,
        title: String = AppConfig.APP_NAME,
        modifier: Modifier = Modifier
    ) {
        invoke(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = JellyfinColors.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(JellyfinDimens.MinTouchTarget)
                    .semantics {
                        role = Role.Button
                        contentDescription = "البحث الفوري"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = JellyfinColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    /**
     * Child Cinema TopBar for internal screens (Library, Details, Profile, etc.):
     * - Standard back button with 48dp touch target
     * - Clean title and optional count / metadata
     * - Optional end actions
     */
    @Composable
    fun Child(
        title: String,
        onBackClick: (() -> Unit)? = null,
        subtitle: String? = null,
        navigationIcon: ImageVector? = null,
        modifier: Modifier = Modifier,
        actions: @Composable RowScope.() -> Unit = {}
    ) {
        invoke(modifier = modifier) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = "الرجوع"
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
            } else if (navigationIcon != null) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = null,
                    tint = JellyfinColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary,
                    maxLines = 1
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary,
                        maxLines = 1
                    )
                }
            }

            actions()
        }
    }
}

// Top-level aliases for ergonomic call sites
@Composable
fun CinemaRootTopBar(
    onSearchClick: () -> Unit,
    title: String = AppConfig.APP_NAME,
    modifier: Modifier = Modifier
) = CinemaTopBar.Root(onSearchClick = onSearchClick, title = title, modifier = modifier)

@Composable
fun CinemaChildTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) = CinemaTopBar.Child(
    title = title,
    onBackClick = onBackClick,
    subtitle = subtitle,
    navigationIcon = navigationIcon,
    modifier = modifier,
    actions = actions
)

val CinemaTopBars = CinemaTopBar
