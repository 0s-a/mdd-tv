package org.jellyfin.mobile.presentation.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Docked Cinema Bottom Navigation Bar adhering to Obsidian 2.2.0 design tokens.
 *
 * Architecture and design features:
 * - Full-width docked bar anchored to screen bottom with true black OLED background.
 * - Top subtle gradient scrim (20dp) fading smoothly from transparent to OLED black,
 *   preventing abrupt cuts as media posters scroll underneath.
 * - Top hairline divider (1dp, JellyfinColors.BorderHairline) marking content boundary.
 * - Minimum 48dp touch targets on all tabs adhering to WCAG 2.1 AA.
 * - Arabic TalkBack accessibility semantics with Role.Tab.
 * - Subtle haptic feedback (TextHandleMove) upon tab clicks.
 * - Dual-callback reselection support: invokes onTabReselected or emits to NavigationEvents.
 * - Active micro-indicator (2dp rounded bar) highlighting the selected tab.
 */
@Composable
fun CinemaBottomBar(
    currentRoute: String,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    onTabReselected: ((NavigationTab) -> Unit)? = null,
    indicatorColor: Color = JellyfinPrimitives.AmberWarm
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Top subtle gradient scrim fading from transparent to OLED black
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            JellyfinPrimitives.BlackOled
                        )
                    )
                )
        )

        // Solid OLED docked container with system navigation bar insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(JellyfinPrimitives.BlackOled)
                .navigationBarsPadding()
        ) {
            // Top hairline border (1dp line with 0x1AFFFFFF)
            HorizontalDivider(
                color = JellyfinColors.BorderHairline,
                thickness = JellyfinDimens.BorderHairline
            )

            // Content row with standard height ~56dp to 64dp (58dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = JellyfinDimens.SpaceMd),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentRoute == tab.route ||
                        (tab == NavigationTab.LIBRARY && currentRoute.startsWith(tab.route))

                    CinemaBottomBarTabItem(
                        tab = tab,
                        isSelected = isSelected,
                        indicatorColor = indicatorColor,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isSelected) {
                                if (onTabReselected != null) {
                                    onTabReselected(tab)
                                } else {
                                    NavigationEvents.emitReselect(tab)
                                }
                            } else {
                                onTabSelected(tab)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual interactive tab item within [CinemaBottomBar].
 */
@Composable
private fun CinemaBottomBarTabItem(
    tab: NavigationTab,
    isSelected: Boolean,
    indicatorColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) JellyfinPrimitives.Ivory50 else JellyfinPrimitives.Gray500,
        animationSpec = tween(durationMillis = 180),
        label = "CinemaBottomBarContentColor"
    )

    val animatedIndicatorColor by animateColorAsState(
        targetValue = if (isSelected) indicatorColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "CinemaBottomBarIndicatorColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = JellyfinDimens.Space2XS)
            .semantics(mergeDescendants = true) {
                this.role = Role.Tab
                this.selected = isSelected
                this.contentDescription = "تبويب ${tab.title}"
            }
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = null,
            tint = animatedContentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = animatedContentColor,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Active micro-indicator: subtle 2dp rounded indicator below icon/text
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMicro))
                .background(animatedIndicatorColor)
        )
    }
}

/**
 * Backward-compatible alias for [NavigationTab].
 */
typealias CinemaNavigationTab = NavigationTab

/**
 * Backward-compatible composable alias for [CinemaBottomBar].
 */
@Composable
fun CinemaBottomNavigationBar(
    currentRoute: String,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    onTabReselected: ((NavigationTab) -> Unit)? = null
) {
    CinemaBottomBar(
        currentRoute = currentRoute,
        onTabSelected = onTabSelected,
        modifier = modifier,
        onTabReselected = onTabReselected
    )
}
