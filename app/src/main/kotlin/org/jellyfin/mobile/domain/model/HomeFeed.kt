package org.jellyfin.mobile.domain.model

import kotlinx.collections.immutable.ImmutableList

/**
 * Pure Kotlin domain model aggregating all data required to render the Home Dashboard.
 */
data class HomeFeed(
    val libraries: ImmutableList<LibraryFolder>,
    val heroItem: MediaItemSummary?,
    val resumeItems: ImmutableList<MediaItemSummary>,
    val latestItems: ImmutableList<MediaItemSummary>
)
