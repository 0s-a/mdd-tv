package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Compact Save Modal Bottom Sheet providing unified options for:
 * 1. Favorite (المفضلة)
 * 2. Watchlist / My List (قائمتي / المشاهدة لاحقاً)
 *
 * Adheres strictly to the master redesign architecture in docs/current/screens/media_details/.
 * Zero emojis in all code and presentation strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveModalBottomSheet(
    isFavorite: Boolean,
    isWatchlist: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JellyfinPrimitives.Graphite900,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(
            topStart = JellyfinDimens.RadiusLarge,
            topEnd = JellyfinDimens.RadiusLarge
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = JellyfinDimens.SpaceLarge)
                .padding(bottom = JellyfinDimens.SpaceLarge)
        ) {
            Text(
                text = "حفظ في القوائم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            // 1. Favorite Option Row
            SaveOptionRow(
                icon = Icons.Rounded.Favorite,
                title = "المفضلة",
                subtitle = "تمييز هذا العمل كعمل مفضل لديك",
                iconTint = if (isFavorite) JellyfinColors.RatingGold else JellyfinColors.TextSecondary,
                isSelected = isFavorite,
                onToggle = onToggleFavorite
            )

            HorizontalDivider(
                color = JellyfinPrimitives.Graphite800,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = JellyfinDimens.SpaceSmall)
            )

            // 2. Watchlist Option Row
            SaveOptionRow(
                icon = Icons.Rounded.Bookmark,
                title = "قائمتي",
                subtitle = "حفظ العمل لمشاهدته في وقت لاحق",
                iconTint = if (isWatchlist) JellyfinColors.TechSky else JellyfinColors.TextSecondary,
                isSelected = isWatchlist,
                onToggle = onToggleWatchlist
            )
        }
    }
}

@Composable
private fun SaveOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = JellyfinDimens.SpaceSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = JellyfinColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = JellyfinColors.RatingGold,
                uncheckedColor = JellyfinPrimitives.Slate600,
                checkmarkColor = JellyfinPrimitives.BlackOled
            )
        )
    }
}
