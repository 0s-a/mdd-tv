package org.jellyfin.mobile.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * Luxury glassmorphic card for rendering folders/categories in the library browser grid.
 * Adheres strictly to Obsidian 2.2.0 design tokens:
 * - 2:3 aspect ratio matching poster cards in the 3-column grid.
 * - SurfaceDark and SurfaceGlass vertical gradient.
 * - Hairline BorderGlass border.
 * - Glowing folder icon (Icons.Rounded.FolderSpecial) with subtle indigo radial glow.
 * - Full TalkBack accessibility and 48dp minimum touch target.
 */
@Composable
fun FolderCard(
    item: MediaItemSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(JellyfinDimens.RadiusMedium)
    val posterUrl = item.posterUrl?.takeIf { it.isNotBlank() } ?: item.backdropUrl?.takeIf { it.isNotBlank() }
    val hasPoster = !posterUrl.isNullOrBlank()
    val isMovieFolder = item.name.contains("فيلم", ignoreCase = true) || item.name.contains("أفلام", ignoreCase = true)
    val isTvFolder = item.name.contains("مسلسل", ignoreCase = true) || item.name.contains("مسلسلات", ignoreCase = true)
    val folderIcon = when {
        isMovieFolder -> Icons.Rounded.Movie
        isTvFolder -> Icons.Rounded.Tv
        else -> Icons.Rounded.FolderSpecial
    }
    val badgeText = when {
        isMovieFolder -> "أفلام"
        isTvFolder -> "مسلسلات"
        else -> "مجلد"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = JellyfinDimens.MinTouchTarget, minHeight = JellyfinDimens.MinTouchTarget)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "مجلد ${item.name}"
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JellyfinColors.SurfaceDark,
                            JellyfinColors.SurfaceGlass
                        )
                    )
                )
                .border(1.dp, JellyfinColors.BorderGlass, shape)
        ) {
            if (hasPoster) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Frosted gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            }

            // Glowing Icon & Folder Title
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(JellyfinDimens.SpaceSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    JellyfinColors.PrimaryIndigo.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.dp, JellyfinColors.PrimaryIndigo.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = folderIcon,
                        contentDescription = null,
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                // Folder Title
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    ),
                    color = JellyfinColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Top Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JellyfinColors.SurfaceDark.copy(alpha = 0.85f))
                    .border(0.5.dp, JellyfinColors.BorderGlass, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = JellyfinColors.TextSecondary
                )
            }
        }
    }
}
