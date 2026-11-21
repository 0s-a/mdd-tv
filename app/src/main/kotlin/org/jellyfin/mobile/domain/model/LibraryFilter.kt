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
 * Filter configuration for querying and narrowing items in a Jellyfin media library.
 * Pure Kotlin with zero Android dependencies.
 */
data class LibraryFilter(
    val sortBy: LibrarySortBy = LibrarySortBy.DATE_ADDED
)
