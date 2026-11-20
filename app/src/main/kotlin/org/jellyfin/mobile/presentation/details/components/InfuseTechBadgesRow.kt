package org.jellyfin.mobile.presentation.details.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.domain.model.MediaDetailDomainModel
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * InfuseTechBadgesRow displays subtle, quiet monochrome badges for
 * hardware playback and audio capabilities (e.g. 4K, HDR, Surround).
 * Strictly complies with the minimalist, clutter-free media details specification.
 */
@Composable
fun InfuseTechBadgesRow(
    mediaItem: MediaDetailDomainModel,
    modifier: Modifier = Modifier
) {
    val badges = mediaItem.qualityBadges.ifEmpty {
        val list = mutableListOf<String>()
        val res = mediaItem.videoResolution ?: "4K UHD"
        list.add(res)
        if (res.contains("4K", ignoreCase = true)) {
            list.add("HDR")
        }
        val aCodec = mediaItem.audioCodec.orEmpty()
        val channels = mediaItem.audioChannels.orEmpty()
        if (channels.contains("5.1") || channels.contains("7.1") || aCodec.contains("5.1") || aCodec.contains("Atmos", ignoreCase = true)) {
            list.add("صوت محيطي")
        }
        list
    }

    if (badges.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = JellyfinDimens.SpaceExtraSmall)
    ) {
        badges.forEach { badge ->
            TechBadgeOutline(badge = badge)
        }
    }
}

@Composable
private fun TechBadgeOutline(
    badge: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(
                width = 1.dp,
                color = JellyfinColors.BorderGlass,
                shape = shape
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextMuted
        )
    }
}
