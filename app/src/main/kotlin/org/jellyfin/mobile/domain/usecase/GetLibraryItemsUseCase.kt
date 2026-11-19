package org.jellyfin.mobile.domain.usecase

import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.repository.LibraryPage
import org.jellyfin.mobile.domain.repository.LibraryRepository

/**
 * Pure Kotlin use case for retrieving filtered and sorted library items.
 */
class GetLibraryItemsUseCase(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(
        parentId: String,
        filter: LibraryFilter = LibraryFilter(),
        letter: Char? = null,
        startIndex: Int = 0,
        limit: Int = 100,
        isHierarchical: Boolean = true
    ): Result<LibraryPage> {
        return libraryRepository.getLibraryItems(
            parentId = parentId,
            filter = filter,
            letter = letter,
            startIndex = startIndex,
            limit = limit,
            isHierarchical = isHierarchical
        )
    }
}
