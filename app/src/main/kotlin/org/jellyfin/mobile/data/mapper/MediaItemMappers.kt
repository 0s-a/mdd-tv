package org.jellyfin.mobile.data.mapper

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.remote.dto.BaseItemDto
import org.jellyfin.mobile.domain.model.ContinueWatchingItem
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * Defensive mappers for transforming remote Jellyfin media items into pure domain models.
 */

fun BaseItemDto.toMediaItemSummary(): MediaItemSummary {
    val itemId = id.orEmpty()
    val isEpisode = type.equals("Episode", ignoreCase = true)

    val displayName = when {
        isEpisode && !seriesName.isNullOrBlank() -> {
            val epNum = indexNumber?.let { "E$it" } ?: ""
            val seasonNum = parentIndexNumber?.let { "S$it" } ?: ""
            val epTag = if (seasonNum.isNotEmpty() || epNum.isNotEmpty()) " ($seasonNum$epNum)" else ""
            "$seriesName$epTag - ${name ?: "حلقة"}"
        }
        else -> name?.takeIf { it.isNotBlank() } ?: seriesName?.takeIf { it.isNotBlank() } ?: "بدون عنوان"
    }

    val percentage = userData?.playedPercentage
        ?: if (runTimeTicks != null && runTimeTicks > 0 && userData?.playbackPositionTicks != null) {
            (userData.playbackPositionTicks.toDouble() / runTimeTicks.toDouble()) * 100.0
        } else 0.0

    val backdrop = when {
        !backdropImageTags.isNullOrEmpty() -> JellyfinApiService.buildBackdropImageUrl(itemId)
        !parentBackdropItemId.isNullOrBlank() -> JellyfinApiService.buildBackdropImageUrl(parentBackdropItemId)
        !seriesId.isNullOrBlank() -> JellyfinApiService.buildBackdropImageUrl(seriesId)
        else -> JellyfinApiService.buildPrimaryImageUrl(itemId)
    }

    val poster = when {
        !seriesPrimaryImageTag.isNullOrBlank() && !seriesId.isNullOrBlank() -> {
            JellyfinApiService.buildPrimaryImageUrl(seriesId)
        }
        else -> JellyfinApiService.buildPrimaryImageUrl(itemId)
    }

    val durationText = runTimeTicks?.let { ticks ->
        val totalMinutes = (ticks / 10_000_000L / 60L).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        when {
            hours > 0 -> "${hours}س ${mins}د"
            mins > 0 -> "${mins}د"
            else -> null
        }
    }

    // Technical Quality Badges (4K UHD, HDR, Dolby Vision, 1080p)
    val badges = mutableListOf<String>()
    mediaSources?.forEach { ms ->
        ms.mediaStreams?.forEach { stream ->
            if (stream.type.equals("Video", ignoreCase = true)) {
                val width = stream.width ?: 0
                val height = stream.height ?: 0
                val title = stream.displayTitle.orEmpty()
                if (width >= 3840 || height >= 2160 || title.contains("4K", ignoreCase = true)) {
                    if (!badges.contains("4K UHD")) badges.add("4K UHD")
                } else if (width >= 1920 || height >= 1080 || title.contains("1080", ignoreCase = true)) {
                    if (!badges.contains("1080p")) badges.add("1080p")
                }

                val range = stream.videoRange.orEmpty()
                val rangeType = stream.videoRangeType.orEmpty()
                if (rangeType.contains("DOVI", ignoreCase = true) || title.contains("DV", ignoreCase = true)) {
                    if (!badges.contains("Dolby Vision")) badges.add("Dolby Vision")
                } else if (range.contains("HDR", ignoreCase = true) || rangeType.contains("HDR", ignoreCase = true)) {
                    if (!badges.contains("HDR")) badges.add("HDR")
                }
            }
        }
    }

    return MediaItemSummary(
        id = itemId,
        name = displayName,
        type = type ?: "Unknown",
        productionYear = productionYear,
        communityRating = communityRating,
        runTimeTicks = runTimeTicks,
        playbackPositionPercentage = percentage,
        backdropImageUrl = backdrop,
        posterImageUrl = poster,
        isPlayed = userData?.played == true,
        overview = overview,
        durationText = durationText,
        genres = genres?.toImmutableList() ?: persistentListOf(),
        qualityBadges = badges.toImmutableList(),
        isFavorite = userData?.isFavorite == true
    )
}

fun MediaItemSummary.toContinueWatchingItem(): ContinueWatchingItem {
    val totalMs = (runTimeTicks ?: 0L) / 10_000L
    val resumeMs = ((totalMs * (playbackPositionPercentage / 100.0))).toLong()
    val remainingMs = (totalMs - resumeMs).coerceAtLeast(0L)
    val remainingMins = (remainingMs / (1000 * 60)).toInt()

    return ContinueWatchingItem(
        id = id,
        title = name,
        subtitle = durationText ?: if (remainingMins > 0) "متبقي $remainingMins دقيقة" else null,
        backdropUrl = backdropImageUrl ?: posterImageUrl,
        posterUrl = posterImageUrl,
        progressPercentage = (playbackPositionPercentage / 100.0).toFloat().coerceIn(0f, 1f),
        remainingMinutes = remainingMins,
        resumePositionMs = resumeMs,
        totalDurationMs = totalMs
    )
}
