package org.jellyfin.mobile.data.remote.api

import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.data.remote.dto.AuthenticateUserByNameDto
import org.jellyfin.mobile.data.remote.dto.AuthenticationResultDto
import org.jellyfin.mobile.data.remote.dto.BaseItemDto
import org.jellyfin.mobile.data.remote.dto.ItemsResponseDto
import org.jellyfin.mobile.data.remote.dto.PlaybackProgressDto
import org.jellyfin.mobile.data.remote.dto.PlaybackStartDto
import org.jellyfin.mobile.data.remote.dto.PlaybackStopDto
import org.jellyfin.mobile.data.remote.dto.PublicSystemInfoDto
import org.jellyfin.mobile.data.remote.dto.UserViewsResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Jellyfin REST API endpoints.
 */
interface JellyfinApiService {

    @GET("System/Info/Public")
    suspend fun getPublicSystemInfo(): PublicSystemInfoDto

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Body body: AuthenticateUserByNameDto
    ): AuthenticationResultDto

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String
    ): UserViewsResponseDto

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Query("mediaTypes") mediaTypes: String = "Video",
        @Query("limit") limit: Int = 12
    ): ItemsResponseDto

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("limit") limit: Int = 16,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,Overview,CommunityRating"
    ): List<BaseItemDto>

    @GET("Shows/NextUp")
    suspend fun getNextUpEpisodes(
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 16,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,SeriesInfo,DateCreated,MediaStreams,Overview"
    ): ItemsResponseDto

    @GET("Users/{userId}/Items")
    suspend fun getSpotlightItems(
        @Path("userId") userId: String,
        @Query("sortBy") sortBy: String = "Random",
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series",
        @Query("imageTypes") imageTypes: String = "Backdrop",
        @Query("recursive") recursive: Boolean = true,
        @Query("limit") limit: Int = 5,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,Overview,CommunityRating,MediaSources,Taglines,Genres"
    ): ItemsResponseDto

    @GET("Users/{userId}/Items")
    suspend fun getSplashItems(
        @Path("userId") userId: String,
        @Query("sortBy") sortBy: String = "DateCreated,SortName",
        @Query("sortOrder") sortOrder: String = "Descending",
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series",
        @Query("imageTypes") imageTypes: String = "Primary",
        @Query("recursive") recursive: Boolean = true,
        @Query("limit") limit: Int = 20,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,Genres,CommunityRating"
    ): ItemsResponseDto

    @GET("Users/{userId}/Items")
    suspend fun getLibraryItems(
        @Path("userId") userId: String,
        @Query("parentId") parentId: String,
        @Query("sortBy") sortBy: String = "DateCreated,SortName",
        @Query("sortOrder") sortOrder: String = "Descending",
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series",
        @Query("filters") filters: String? = null,
        @Query("startIndex") startIndex: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,Overview,CommunityRating,MediaSources",
        @Query("recursive") recursive: Boolean = true,
        @Query("nameStartsWith") nameStartsWith: String? = null
    ): ItemsResponseDto

    @GET("Genres")
    suspend fun getGenres(
        @Query("userId") userId: String? = null,
        @Query("parentId") parentId: String? = null,
        @Query("recursive") recursive: Boolean = true,
        @Query("sortBy") sortBy: String = "SortName"
    ): ItemsResponseDto

    @GET("Users/{userId}/Items")
    suspend fun searchMedia(
        @Path("userId") userId: String,
        @Query("searchTerm") searchTerm: String? = null,
        @Query("genres") genres: String? = null,
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series,Person",
        @Query("recursive") recursive: Boolean = true,
        @Query("limit") limit: Int = 30,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,UserData,Overview,Genres,People"
    ): ItemsResponseDto

    @GET("Users/{userId}/Items")
    suspend fun getItemsByGenre(
        @Path("userId") userId: String,
        @Query("genres") genres: String,
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series",
        @Query("recursive") recursive: Boolean = true,
        @Query("limit") limit: Int = 50,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,UserData,Overview,Genres"
    ): ItemsResponseDto

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): BaseItemDto

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String
    ): ItemsResponseDto

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String,
        @Query("seasonId") seasonId: String? = null,
        @Query("fields") fields: String = "PrimaryImageAspectRatio,Overview,CommunityRating,MediaSources"
    ): ItemsResponseDto

    @GET("Items/{itemId}/Similar")
    suspend fun getSimilarItems(
        @Path("itemId") itemId: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 12
    ): ItemsResponseDto

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): BaseItemDto

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun removeFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): BaseItemDto

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): BaseItemDto

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markUnplayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): BaseItemDto

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(
        @Body body: PlaybackStartDto
    ): retrofit2.Response<Unit>

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Body body: PlaybackProgressDto
    ): retrofit2.Response<Unit>

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Body body: PlaybackStopDto
    ): retrofit2.Response<Unit>

    companion object {
        fun buildPrimaryImageUrl(
            itemId: String,
            maxWidth: Int? = null,
            maxHeight: Int? = null,
            quality: Int = 90
        ): String {
            val base = AppConfig.SERVER_URL.trimEnd('/')
            val builder = StringBuilder("$base/Items/$itemId/Images/Primary?quality=$quality")
            if (maxWidth != null) builder.append("&maxWidth=$maxWidth")
            if (maxHeight != null) builder.append("&maxHeight=$maxHeight")
            return builder.toString()
        }

        fun buildBackdropImageUrl(
            itemId: String,
            maxWidth: Int? = 1920,
            quality: Int = 85
        ): String {
            val base = AppConfig.SERVER_URL.trimEnd('/')
            val builder = StringBuilder("$base/Items/$itemId/Images/Backdrop?quality=$quality")
            if (maxWidth != null) builder.append("&maxWidth=$maxWidth")
            return builder.toString()
        }
    }
}
