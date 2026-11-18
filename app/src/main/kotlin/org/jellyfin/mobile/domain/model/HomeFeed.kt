package org.jellyfin.mobile.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Pure Kotlin domain model aggregating all data required to render the Home Dashboard.
 */
data class HomeFeed(
    val libraries: ImmutableList<LibraryFolder>,
    val heroItems: ImmutableList<MediaItemSummary>,
    val resumeItems: ImmutableList<MediaItemSummary>,
    val latestItems: ImmutableList<MediaItemSummary>
) {
    constructor(
        libraries: ImmutableList<LibraryFolder>,
        heroItem: MediaItemSummary?,
        resumeItems: ImmutableList<MediaItemSummary>,
        latestItems: ImmutableList<MediaItemSummary>
    ) : this(
        libraries = libraries,
        heroItems = heroItem?.let { persistentListOf(it) } ?: persistentListOf(),
        resumeItems = resumeItems,
        latestItems = latestItems
    )

    val heroItem: MediaItemSummary? get() = heroItems.firstOrNull()
}
