package org.jellyfin.mobile.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.BreadcrumbItem
import org.jellyfin.mobile.domain.model.LibrarySortBy
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * BreadcrumbsBar displays the hierarchical navigation trail with one-tap ancestor jumping,
 * an instant back button for parent folder navigation, and a compact sort toggle.
 */
@Composable
fun BreadcrumbsBar(
    breadcrumbs: ImmutableList<BreadcrumbItem>,
    onBreadcrumbClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSort: LibrarySortBy? = null,
    onSortClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = 4.dp)
    ) {
        // Leading section: Back button & Breadcrumbs trail
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Back button: shown when inside subfolders (depth > 1)
            if (breadcrumbs.size > 1) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.SurfaceGlass)
                        .border(1.dp, JellyfinColors.BorderGlass, CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = onBackClick
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "الرجوع للمجلد السابق"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Clean breadcrumb trail
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(breadcrumbs) { index, item ->
                    val isLast = index == breadcrumbs.size - 1

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                enabled = !isLast,
                                role = Role.Button,
                                onClick = { onBreadcrumbClick(index) }
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = item.name
                            }
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            ),
                            color = if (isLast) JellyfinColors.TechSky else JellyfinColors.TextSecondary
                        )

                        if (!isLast) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = JellyfinColors.TextMuted,
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Trailing section: Compact Sort Selector
        if (selectedSort != null && onSortClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
                    .background(JellyfinColors.SurfaceGlass)
                    .border(1.dp, JellyfinColors.BorderGlass, RoundedCornerShape(JellyfinDimens.RadiusPill))
                    .clickable(role = Role.Button, onClick = onSortClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "الترتيب حسب: ${selectedSort.title}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = null,
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = selectedSort.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = JellyfinColors.TextPrimary
                    )
                }
            }
        }
    }
}
