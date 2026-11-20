package org.jellyfin.mobile.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * LibraryCapsulesRow renders dynamic, server-driven library pills.
 * Strictly adheres to 01_HOME_DASHBOARD.md Section 2:
 * - 100% server-driven via GET /Users/{userId}/Views.
 * - Glassmorphic pills with >= 48dp touch target for accessibility.
 * - Arabic TalkBack semantics.
 */
@Composable
fun LibraryCapsulesRow(
    libraries: ImmutableList<LibraryFolder>,
    onLibraryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (libraries.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "المكتبات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            modifier = Modifier.padding(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = libraries,
                key = { it.id }
            ) { folder ->
                LibraryCapsule(
                    folder = folder,
                    onClick = { onLibraryClick(folder.id) }
                )
            }
        }
    }
}

@Composable
private fun LibraryCapsule(
    folder: LibraryFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (folder.type.lowercase()) {
        "movies" -> Icons.Rounded.Movie
        "tvshows" -> Icons.Rounded.Tv
        "music" -> Icons.Rounded.MusicNote
        "photos" -> Icons.Rounded.Photo
        "books" -> Icons.Rounded.Book
        else -> Icons.Rounded.Folder
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(
                minWidth = JellyfinDimens.MinTouchTarget,
                minHeight = JellyfinDimens.MinTouchTarget
            )
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceGlass,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusPill
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "تصفح مكتبة ${folder.name}"
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JellyfinColors.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextPrimary
        )
    }
}
