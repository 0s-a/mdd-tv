package org.jellyfin.mobile.presentation.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

enum class NavigationTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "home",
        title = "الرئيسية",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    LIBRARY(
        route = "library",
        title = "المكتبة",
        selectedIcon = Icons.Rounded.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary
    ),
    PROFILE(
        route = "profile",
        title = "الشخصي",
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

/**
 * Deprecated floating bottom navigation bar.
 * Replaced by CinemaBottomBar for OLED Obsidian 2.2.0 compliance.
 */
@Deprecated(
    message = "Use CinemaBottomBar instead",
    replaceWith = ReplaceWith("CinemaBottomBar(currentRoute, onTabSelected, modifier)")
)
@Composable
fun FloatingBottomNavigationBar(
    currentRoute: String,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    CinemaBottomBar(
        currentRoute = currentRoute,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}

@Composable
private fun BottomNavigationTabItem(
    tab: NavigationTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "BottomNavTabColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = JellyfinDimens.SpaceExtraSmall)
            .semantics(mergeDescendants = true) {
                this.role = Role.Tab
                this.selected = isSelected
                this.contentDescription = "تبويب ${tab.title}"
            }
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = null,
            tint = animatedColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = animatedColor
        )
    }
}
