package org.jellyfin.mobile.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * FrostedSearchBar provides a glassmorphic search input with clear action, trailing filter button,
 * and active multi-genre removable chips row.
 */
@Composable
fun FrostedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedGenres: ImmutableSet<String> = persistentSetOf(),
    onRemoveGenre: ((String) -> Unit)? = null,
    onClearGenres: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    autoFocus: Boolean = false,
    placeholder: String = "ابحث عن فيلم، مسلسل، ممثل..."
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .glassmorphism(
                    backgroundColor = JellyfinColors.SurfaceGlass,
                    borderColor = JellyfinColors.BorderGlass,
                    cornerRadius = JellyfinDimens.RadiusPill,
                    borderWidth = JellyfinDimens.BorderThin
                )
                .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = 2.dp)
        ) {
            // Optional Back Navigation Button
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Button
                            contentDescription = "إغلاق نافذة البحث"
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceExtraSmall))
            }

            // Search Icon
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = JellyfinColors.TextSecondary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(22.dp)
            )

            Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))

            // Input Field
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JellyfinColors.TextMuted,
                        maxLines = 1
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(JellyfinColors.AccentAmber),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = JellyfinColors.TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics {
                            contentDescription = "حقل البحث، اكتب للبحث عن الأفلام والمسلسلات والممثلين"
                        }
                )
            }

            // Clear Query Button
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Button
                            contentDescription = "مسح نص البحث"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = null,
                        tint = JellyfinColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Trailing Dynamic Genre Filter Button
            if (onFilterClick != null) {
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Button
                            contentDescription = "فلتر تصنيفات السيرفر"
                        }
                ) {
                    if (selectedGenres.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = JellyfinColors.AccentAmber,
                                    contentColor = JellyfinColors.PureBlack
                                ) {
                                    Text(
                                        text = selectedGenres.size.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = JellyfinColors.AccentAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = JellyfinColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Active Removable Genre Chips Row
        if (selectedGenres.isNotEmpty() && onRemoveGenre != null) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = JellyfinDimens.SpaceSmall),
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(selectedGenres.toList(), key = { it }) { genre ->
                    ActiveGenreRemovableChip(
                        genre = genre,
                        onRemove = { onRemoveGenre(genre) }
                    )
                }

                if (onClearGenres != null && selectedGenres.size > 1) {
                    item(key = "clear_all_chips") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
                                .background(JellyfinColors.SurfaceElevated)
                                .clickable(onClick = onClearGenres)
                                .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "مسح الفلاتر",
                                style = MaterialTheme.typography.labelSmall,
                                color = JellyfinColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveGenreRemovableChip(
    genre: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
            .background(JellyfinColors.AccentAmber.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = JellyfinColors.AccentAmber,
                shape = RoundedCornerShape(JellyfinDimens.RadiusPill)
            )
            .clickable(onClick = onRemove)
            .padding(
                start = JellyfinDimens.SpaceMedium,
                end = JellyfinDimens.SpaceSmall,
                top = 5.dp,
                bottom = 5.dp
            )
            .semantics {
                role = Role.Button
                contentDescription = "إزالة تصنيف $genre"
            }
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.AccentAmber
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = JellyfinColors.AccentAmber,
            modifier = Modifier.size(14.dp)
        )
    }
}
