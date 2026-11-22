package org.jellyfin.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserViewsResponseDto(
    @SerialName("Items") val items: List<BaseItemDto>? = null,
    @SerialName("TotalRecordCount") val totalRecordCount: Int? = null,
    @SerialName("StartIndex") val startIndex: Int? = null
)

@Serializable
data class BaseItemDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("ServerId") val serverId: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("CollectionType") val collectionType: String? = null,
    @SerialName("IsFolder") val isFolder: Boolean? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("CommunityRating") val communityRating: Float? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("UserData") val userData: UserDataDto? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeasonName") val seasonName: String? = null,
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeasonId") val seasonId: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("PrimaryImageAspectRatio") val primaryImageAspectRatio: Double? = null,
    @SerialName("SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
    @SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String>? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerialName("Genres") val genres: List<String>? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSourceDto>? = null,
    @SerialName("Taglines") val taglines: List<String>? = null,
    @SerialName("People") val people: List<PersonDto>? = null,
    @SerialName("ChildCount") val childCount: Int? = null,
    @SerialName("Chapters") val chapters: List<ChapterInfoDto>? = null
)

@Serializable
data class ChapterInfoDto(
    @SerialName("StartPositionTicks") val startPositionTicks: Long? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("ImageTag") val imageTag: String? = null
)

@Serializable
data class PersonDto(
    @SerialName("Name") val name: String? = null,
    @SerialName("Id") val id: String? = null,
    @SerialName("Role") val role: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null
)

@Serializable
data class UserDataDto(
    @SerialName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long? = null,
    @SerialName("PlayCount") val playCount: Int? = null,
    @SerialName("IsFavorite") val isFavorite: Boolean? = null,
    @SerialName("Played") val played: Boolean? = null,
    @SerialName("UnplayedItemCount") val unplayedItemCount: Int? = null,
    @SerialName("Key") val key: String? = null
)

@Serializable
data class MediaSourceDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("Path") val path: String? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("Bitrate") val bitrate: Int? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamDto>? = null
)

@Serializable
data class MediaStreamDto(
    @SerialName("Type") val type: String? = null,
    @SerialName("Index") val index: Int? = null,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Language") val language: String? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    @SerialName("IsDefault") val isDefault: Boolean? = null,
    @SerialName("IsForced") val isForced: Boolean? = null,
    @SerialName("IsExternal") val isExternal: Boolean? = null,
    @SerialName("DeliveryUrl") val deliveryUrl: String? = null,
    @SerialName("VideoRange") val videoRange: String? = null,
    @SerialName("VideoRangeType") val videoRangeType: String? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null,
    @SerialName("AspectRatio") val aspectRatio: String? = null,
    @SerialName("AverageFrameRate") val averageFrameRate: Float? = null,
    @SerialName("Channels") val channels: Int? = null,
    @SerialName("ChannelLayout") val channelLayout: String? = null,
    @SerialName("BitRate") val bitRate: Int? = null
)
