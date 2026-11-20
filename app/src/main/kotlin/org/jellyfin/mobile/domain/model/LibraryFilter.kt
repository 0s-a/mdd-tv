package org.jellyfin.mobile.domain.model

/**
 * Sorting criteria for library catalog items.
 * Pure Kotlin with zero Android dependencies.
 */
enum class LibrarySortBy(val title: String) {
    DATE_ADDED("الأحدث إضافة"),
    NAME("الاسم أبجدياً"),
    RATING("تقييم الجمهور"),
    PREMIERE_DATE("تاريخ الإصدار")
}

// Backward compatibility alias
typealias SortOption = LibrarySortBy

/**
 * Quick filter chips for UI bar.
 */
enum class LibraryFilterChip(val title: String) {
    UNWATCHED("غير المشاهد"),
    ONLY_4K("4K UHD فقط"),
    HIGH_RATING("تقييم +8.0"),
    FAVORITES("المفضلة")
}

/**
 * Filter configuration for querying and narrowing items in a Jellyfin media library.
 * Pure Kotlin with zero Android dependencies.
 */
data class LibraryFilter(
    val sortBy: LibrarySortBy = LibrarySortBy.DATE_ADDED,
    val isUnwatchedOnly: Boolean = false,
    val is4kOnly: Boolean = false,
    val isFavoriteOnly: Boolean = false,
    val minRating: Float? = null
)
