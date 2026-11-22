package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Top Master Actions Overflow Menu for all Media Details Screens.
 * Contains:
 * 1. Playback Options (Audio & Subtitles & External Player)
 * 2. Share
 * 3. Toggle Watched
 * 4. Trailer (strictly conditional)
 *
 * Zero emojis in all code and presentation strings.
 */
@Composable
fun TopActionsDropdownMenu(
    onPlaybackOptionsClick: () -> Unit,
    onShareClick: () -> Unit,
    isWatched: Boolean,
    onToggleWatchedClick: () -> Unit,
    hasTrailer: Boolean,
    onTrailerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier
                .size(JellyfinDimens.MinTouchTarget)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.40f))
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "قائمة الإجراءات الإضافية",
                tint = JellyfinPrimitives.Ivory50,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(JellyfinPrimitives.Graphite900)
        ) {
            // 1. Audio & Subtitle Options
            DropdownMenuItem(
                text = {
                    Text(
                        text = "خيارات الصوت والترجمة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JellyfinColors.TextPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = JellyfinColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    isExpanded = false
                    onPlaybackOptionsClick()
                }
            )

            // 2. Share Action
            DropdownMenuItem(
                text = {
                    Text(
                        text = "مشاركة العمل",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JellyfinColors.TextPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = JellyfinColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    isExpanded = false
                    onShareClick()
                }
            )

            // 3. Watched Toggle Action
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isWatched) "تحديد كـ غير مشاهد" else "تحديد كـ تمت المشاهدة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isWatched) Icons.Rounded.Check else Icons.Outlined.Check,
                        contentDescription = null,
                        tint = if (isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    isExpanded = false
                    onToggleWatchedClick()
                }
            )

            // 4. Trailer (strictly conditional)
            if (hasTrailer) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "المقطع الدعائي",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JellyfinColors.TextPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Movie,
                            contentDescription = null,
                            tint = JellyfinColors.RatingGold,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onTrailerClick()
                    }
                )
            }
        }
    }
}
