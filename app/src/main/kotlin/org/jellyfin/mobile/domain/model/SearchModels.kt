package org.jellyfin.mobile.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Domain model representing search results grouped into top match, media titles, and people.
 * Pure Kotlin with zero android.* dependencies.
 */
data class SearchResultSummary(
    val query: String,
    val topMatch: MediaItemSummary? = null,
    val mediaResults: ImmutableList<MediaItemSummary> = persistentListOf(),
    val peopleResults: ImmutableList<CastMember> = persistentListOf(),
    val totalCount: Int = 0
)

/**
 * Person summary representation for UI results presentation.
 * Pure Kotlin with zero android.* dependencies.
 */
data class PersonSummary(
    val id: String,
    val name: String,
    val role: String? = null,
    val avatarUrl: String? = null,
    val workCount: Int = 0
)

fun CastMember.toPersonSummary(): PersonSummary = PersonSummary(
    id = id,
    name = name,
    role = role,
    avatarUrl = imageUrl,
    workCount = 0
)

/**
 * Domain model for exploring media by genre capsules/cards in Idle explore state.
 * Pure Kotlin with zero android.* dependencies.
 */
data class GenreCategory(
    val id: String,
    val name: String,
    val gradientStartHex: Long = 0xFF0D47A1L,
    val gradientEndHex: Long = 0xFF00E5FFL,
    val iconName: String = "Explore"
) {
    companion object {
        fun getDefaultExploreGenres(): List<GenreCategory> = listOf(
            GenreCategory(
                id = "scifi",
                name = "خيال علمي",
                gradientStartHex = 0xFF0F172AL,
                gradientEndHex = 0xFF38BDF8L,
                iconName = "RocketLaunch"
            ),
            GenreCategory(
                id = "action",
                name = "حركة وأكشن",
                gradientStartHex = 0xFF2E0808L,
                gradientEndHex = 0xFFF97316L,
                iconName = "FlashOn"
            ),
            GenreCategory(
                id = "drama",
                name = "دراما سينمائية",
                gradientStartHex = 0xFF1E1035L,
                gradientEndHex = 0xFFA855F7L,
                iconName = "TheaterComedy"
            ),
            GenreCategory(
                id = "horror",
                name = "رعب وغموض",
                gradientStartHex = 0xFF090D16L,
                gradientEndHex = 0xFF0EA5E9L,
                iconName = "VisibilityOff"
            ),
            GenreCategory(
                id = "animation",
                name = "رسوم متحركة",
                gradientStartHex = 0xFF06281DL,
                gradientEndHex = 0xFF14B8A6L,
                iconName = "Animation"
            ),
            GenreCategory(
                id = "documentary",
                name = "وثائقي",
                gradientStartHex = 0xFF271B04L,
                gradientEndHex = 0xFFF59E0BL,
                iconName = "Explore"
            )
        )
    }
}
