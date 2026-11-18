package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * CastCarousel displays circular cast avatars in an accessible horizontal carousel.
 * Strictly adheres to docs/screens/03_MEDIA_DETAILS.md Section 8.
 */
@Composable
fun CastCarousel(
    castList: ImmutableList<CastMember>,
    onCastClick: (CastMember) -> Unit,
    modifier: Modifier = Modifier
) {
    if (castList.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "طاقم التمثيل",
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
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = castList,
                key = { it.id }
            ) { member ->
                CastMemberItem(
                    member = member,
                    onClick = { onCastClick(member) }
                )
            }
        }
    }
}

@Composable
private fun CastMemberItem(
    member: CastMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val talkBackDesc = "${member.name} بدور ${member.role ?: "شخصية"}، اضغط لعرض أعماله"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(84.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            }
    ) {
        // Circular Avatar Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(JellyfinColors.SurfaceDark)
        ) {
            if (!member.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = JellyfinColors.TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Actor Name
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Character Role
        if (!member.role.isNullOrBlank()) {
            Text(
                text = member.role,
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
