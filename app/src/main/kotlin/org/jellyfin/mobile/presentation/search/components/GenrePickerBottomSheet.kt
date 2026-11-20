package org.jellyfin.mobile.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * GenrePickerBottomSheet displays a dynamic multi-select bottom sheet for server genres.
 * 100% dynamic from Jellyfin server with zero hardcoded values.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenrePickerBottomSheet(
    availableGenres: ImmutableList<String>,
    selectedGenres: ImmutableSet<String>,
    onToggleGenre: (String) -> Unit,
    onClearGenres: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JellyfinColors.SurfaceDark,
        shape = RoundedCornerShape(
            topStart = JellyfinDimens.RadiusLarge,
            topEnd = JellyfinDimens.RadiusLarge
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = JellyfinDimens.SpaceLarge)
                .padding(bottom = JellyfinDimens.SpaceLarge)
        ) {
            // Header Row: Title, active counter, and clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تصنيفات السيرفر",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary
                    )
                    if (selectedGenres.isNotEmpty()) {
                        Text(
                            text = "تم اختيار ${selectedGenres.size} من التصنيفات",
                            style = MaterialTheme.typography.labelSmall,
                            color = JellyfinColors.AccentCyan
                        )
                    }
                }

                if (selectedGenres.isNotEmpty()) {
                    TextButton(onClick = onClearGenres) {
                        Text(
                            text = "مسح الكل",
                            style = MaterialTheme.typography.labelMedium,
                            color = JellyfinColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            // Body: Scrollable FlowRow of dynamic genres
            if (availableGenres.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = JellyfinColors.AccentCyan,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                        verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
                    ) {
                        availableGenres.forEach { genre ->
                            val isSelected = selectedGenres.contains(genre)
                            GenreSelectableChip(
                                label = genre,
                                isSelected = isSelected,
                                onClick = { onToggleGenre(genre) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

            // Footer Action: Apply button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.AccentCyan,
                    contentColor = JellyfinColors.PureBlack
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JellyfinDimens.MinTouchTarget)
            ) {
                Text(
                    text = if (selectedGenres.isEmpty()) "إغلاق" else "تطبيق (${selectedGenres.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GenreSelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        JellyfinColors.AccentCyan.copy(alpha = 0.18f)
    } else {
        JellyfinColors.SurfaceElevated
    }

    val borderColor = if (isSelected) {
        JellyfinColors.AccentCyan
    } else {
        JellyfinColors.BorderGlass
    }

    val textColor = if (isSelected) {
        JellyfinColors.AccentCyan
    } else {
        JellyfinColors.TextPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(JellyfinDimens.RadiusPill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = 8.dp)
            .semantics {
                role = Role.Checkbox
                selected = isSelected
                contentDescription = "تصنيف $label"
            }
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = JellyfinColors.AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
