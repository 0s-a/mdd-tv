package org.jellyfin.mobile.data.mapper

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.remote.dto.BaseItemDto
import org.jellyfin.mobile.data.remote.dto.PersonDto
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.model.MediaTechBadges
import org.jellyfin.mobile.domain.model.SeasonSummary

/**
 * Defensive mappers for transforming Jellyfin Media Details DTOs into domain models.
 */

fun PersonDto.toCastMember(): CastMember {
    val personId = id.orEmpty()
    val imgUrl = if (personId.isNotBlank()) {
        JellyfinApiService.buildPrimaryImageUrl(personId, maxWidth = 200, maxHeight = 200, quality = 80)
    } else null

    return CastMember(
        id = personId,
        name = name ?: "بدون اسم",
        role = role,
        type = type,
        imageUrl = imgUrl
    )
}

fun BaseItemDto.toSeasonSummary(): SeasonSummary {
    return SeasonSummary(
        id = id.orEmpty(),
        name = name ?: "الموسم ${indexNumber ?: 1}",
        indexNumber = indexNumber,
        episodeCount = childCount ?: userData?.unplayedItemCount ?: 0
    )
}

fun BaseItemDto.toEpisodeSummary(): EpisodeSummary {
    val itemId = id.orEmpty()
    val percentage = userData?.playedPercentage?.toFloat()
        ?: if (runTimeTicks != null && runTimeTicks > 0 && userData?.playbackPositionTicks != null) {
            ((userData.playbackPositionTicks.toDouble() / runTimeTicks.toDouble()) * 100.0).toFloat()
        } else 0f

    val imgUrl = if (itemId.isNotBlank()) {
        JellyfinApiService.buildPrimaryImageUrl(itemId, maxWidth = 400, quality = 80)
    } else null

    return EpisodeSummary(
        id = itemId,
        name = name ?: "الحلقة ${indexNumber ?: 1}",
        seasonId = seasonId,
        indexNumber = indexNumber,
        overview = overview,
        runTimeTicks = runTimeTicks,
        playbackPositionPercentage = percentage,
        isPlayed = userData?.played == true,
        imageUrl = imgUrl
    )
}

fun BaseItemDto.extractTechBadges(): MediaTechBadges {
    var resolution = "1080p FHD"
    var videoCodec = "H.264"
    var dynamicRange = "SDR"
    var audioCodec = "AAC"
    var audioChannels = "2.0"

    val firstSource = mediaSources?.firstOrNull()
    val streams = firstSource?.mediaStreams ?: emptyList()

    val videoStream = streams.firstOrNull { it.type.equals("Video", ignoreCase = true) }
    if (videoStream != null) {
        val width = videoStream.width ?: 0
        val height = videoStream.height ?: 0
        val title = videoStream.displayTitle.orEmpty()

        resolution = when {
            width >= 3840 || height >= 2160 || title.contains("4K", ignoreCase = true) -> "4K UHD"
            width >= 1920 || height >= 1080 || title.contains("1080", ignoreCase = true) -> "1080p FHD"
            width >= 1280 || height >= 720 || title.contains("720", ignoreCase = true) -> "720p HD"
            else -> "SD"
        }

        videoCodec = videoStream.codec?.uppercase() ?: "H.264"

        val range = videoStream.videoRange.orEmpty()
        val rangeType = videoStream.videoRangeType.orEmpty()
        dynamicRange = when {
            rangeType.contains("DOVI", ignoreCase = true) || title.contains("DV", ignoreCase = true) -> "Dolby Vision"
            rangeType.contains("HDR10+", ignoreCase = true) -> "HDR10+"
            range.contains("HDR", ignoreCase = true) || rangeType.contains("HDR", ignoreCase = true) -> "HDR10"
            else -> "SDR"
        }
    }

    val audioStream = streams.firstOrNull { it.type.equals("Audio", ignoreCase = true) }
    if (audioStream != null) {
        audioCodec = audioStream.codec?.uppercase() ?: "AAC"
        val channels = audioStream.channels ?: 2
        val layout = audioStream.channelLayout.orEmpty()
        audioChannels = when {
            channels == 8 || layout.contains("7.1") -> "7.1"
            channels == 6 || layout.contains("5.1") -> "5.1"
            channels == 2 -> "2.0"
            else -> "$channels.0"
        }
    }

    return MediaTechBadges(
        resolution = resolution,
        videoCodec = videoCodec,
        dynamicRange = dynamicRange,
        audioCodec = audioCodec,
        audioChannels = audioChannels,
        isDirectPlay = true
    )
}

fun BaseItemDto.toMediaDetails(
    seasons: List<SeasonSummary> = emptyList(),
    episodes: List<EpisodeSummary> = emptyList(),
    similarItems: List<MediaItemSummary> = emptyList()
): MediaDetails {
    val itemId = id.orEmpty()
    val isMovieType = !type.equals("Series", ignoreCase = true)

    val percentage = userData?.playedPercentage?.toFloat()
        ?: if (runTimeTicks != null && runTimeTicks > 0 && userData?.playbackPositionTicks != null) {
            ((userData.playbackPositionTicks.toDouble() / runTimeTicks.toDouble()) * 100.0).toFloat()
        } else 0f

    val backdrop = when {
        !backdropImageTags.isNullOrEmpty() -> JellyfinApiService.buildBackdropImageUrl(itemId, maxWidth = 1280, quality = 80)
        !parentBackdropItemId.isNullOrBlank() -> JellyfinApiService.buildBackdropImageUrl(parentBackdropItemId, maxWidth = 1280, quality = 80)
        !seriesId.isNullOrBlank() -> JellyfinApiService.buildBackdropImageUrl(seriesId, maxWidth = 1280, quality = 80)
        else -> null
    }

    val poster = when {
        !seriesPrimaryImageTag.isNullOrBlank() && !seriesId.isNullOrBlank() -> {
            JellyfinApiService.buildPrimaryImageUrl(seriesId, maxWidth = 450, quality = 80)
        }
        else -> JellyfinApiService.buildPrimaryImageUrl(itemId, maxWidth = 450, quality = 80)
    }

    val cast = people?.map { it.toCastMember() }?.toImmutableList() ?: persistentListOf()

    return MediaDetails(
        id = itemId,
        title = name?.takeIf { it.isNotBlank() } ?: seriesName?.takeIf { it.isNotBlank() } ?: "بدون عنوان",
        overview = overview.orEmpty(),
        tagline = taglines?.firstOrNull(),
        productionYear = productionYear,
        communityRating = communityRating,
        genres = genres?.toImmutableList() ?: persistentListOf(),
        runTimeTicks = runTimeTicks,
        backdropUrl = backdrop,
        posterUrl = poster,
        isMovie = isMovieType,
        techBadges = extractTechBadges(),
        castMembers = cast,
        seasons = seasons.toImmutableList(),
        episodes = episodes.toImmutableList(),
        similarItems = similarItems.toImmutableList(),
        playbackPositionPercentage = percentage,
        isFavorite = userData?.isFavorite == true
    )
}
