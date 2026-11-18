package org.jellyfin.mobile.presentation.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * FrostedSearchBar provides a glassmorphic search input with clear action and optional back navigation.
 * Strictly adheres to docs/v2/05_SEARCH_AND_EXPLORE_V2.md:
 * - Glassmorphic capsule with 70% opacity and subtle 1dp cyan glass outline.
 * - Minimum 48dp touch targets on all interactive actions.
 * - Arabic TalkBack semantics.
 * - Zero voice search distractions.
 */
@Composable
fun FrostedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
                cursorBrush = SolidColor(JellyfinColors.PrimaryIndigo),
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
                        contentDescription = "مسح نص البحث الحالي"
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
    }
}
