package org.jellyfin.mobile.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.LibraryFilterChip
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.LibrarySortBy
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.usecase.GetLibraryItemsUseCase

/**
 * LibraryViewModel manages state and intents for the Library Browser Screen.
 * Implements MVI pattern strictly adhering to docs/screens/02_LIBRARY_BROWSER.md.
 */
class LibraryViewModel(
    private val getLibraryItemsUseCase: GetLibraryItemsUseCase,
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LibraryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentLibraryId: String = ""
    private var currentLibraryName: String = "المكتبة"
    private var cachedLibraries: ImmutableList<LibraryFolder> = persistentListOf()
    private var currentFilter = LibraryFilter()
    private var activeFilterChips: PersistentSet<LibraryFilterChip> = persistentSetOf()
    private var currentLetter: Char? = null
    private var initialJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadInitialLibrary()
    }

    fun processIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.LoadLibrary -> {
                initialJob?.cancel()
                currentLibraryId = intent.parentId
                if (intent.libraryName.isNotBlank()) currentLibraryName = intent.libraryName
                loadItems()
            }
            is LibraryIntent.SwitchLibrary -> {
                initialJob?.cancel()
                currentLibraryId = intent.libraryId
                if (intent.libraryName.isNotBlank()) currentLibraryName = intent.libraryName
                currentLetter = null
                loadItems()
            }
            is LibraryIntent.ChangeSort -> {
                currentFilter = currentFilter.copy(sortBy = intent.sortBy)
                loadItems()
            }
            is LibraryIntent.ToggleUnwatchedOnly -> {
                currentFilter = currentFilter.copy(isUnwatchedOnly = !currentFilter.isUnwatchedOnly)
                updateChipState(LibraryFilterChip.UNWATCHED, currentFilter.isUnwatchedOnly)
                loadItems()
            }
            is LibraryIntent.Toggle4kOnly -> {
                currentFilter = currentFilter.copy(is4kOnly = !currentFilter.is4kOnly)
                updateChipState(LibraryFilterChip.ONLY_4K, currentFilter.is4kOnly)
                loadItems()
            }
            is LibraryIntent.FilterByLetter -> {
                currentLetter = intent.letter
                loadItems()
            }
            is LibraryIntent.JumpToLetter -> {
                val current = _uiState.value
                if (current is LibraryUiState.Success) {
                    val targetIndex = current.items.indexOfFirst { item ->
                        val firstChar = item.name.firstOrNull()?.uppercaseChar()
                        if (intent.character == '#') {
                            firstChar != null && !firstChar.isLetter()
                        } else {
                            firstChar == intent.character.uppercaseChar()
                        }
                    }
                    if (targetIndex >= 0) {
                        viewModelScope.launch {
                            _effects.send(LibraryEffect.ScrollToIndex(targetIndex))
                        }
                    }
                }
            }
            is LibraryIntent.ToggleFilter -> {
                when (intent.filter) {
                    LibraryFilterChip.UNWATCHED -> {
                        currentFilter = currentFilter.copy(isUnwatchedOnly = !currentFilter.isUnwatchedOnly)
                        updateChipState(LibraryFilterChip.UNWATCHED, currentFilter.isUnwatchedOnly)
                    }
                    LibraryFilterChip.ONLY_4K -> {
                        currentFilter = currentFilter.copy(is4kOnly = !currentFilter.is4kOnly)
                        updateChipState(LibraryFilterChip.ONLY_4K, currentFilter.is4kOnly)
                    }
                    LibraryFilterChip.HIGH_RATING -> {
                        val newMin = if (currentFilter.minRating == null) 8.0f else null
                        currentFilter = currentFilter.copy(minRating = newMin)
                        updateChipState(LibraryFilterChip.HIGH_RATING, newMin != null)
                    }
                    LibraryFilterChip.FAVORITES -> {
                        val newFav = !currentFilter.isFavoriteOnly
                        currentFilter = currentFilter.copy(isFavoriteOnly = newFav)
                        updateChipState(LibraryFilterChip.FAVORITES, newFav)
                    }
                }
                loadItems()
            }
            is LibraryIntent.ResetFilters -> {
                currentFilter = LibraryFilter()
                activeFilterChips = persistentSetOf()
                currentLetter = null
                loadItems()
            }
            is LibraryIntent.Refresh -> {
                loadItems(isRefresh = true)
            }
            is LibraryIntent.OpenMediaDetails -> {
                viewModelScope.launch {
                    _effects.send(LibraryEffect.NavigateToDetails(intent.mediaId))
                }
            }
            is LibraryIntent.QuickPlay -> {
                viewModelScope.launch {
                    _effects.send(LibraryEffect.LaunchPlayer(intent.mediaId, 0L))
                }
            }
            is LibraryIntent.QuickPlayMedia -> {
                viewModelScope.launch {
                    _effects.send(LibraryEffect.LaunchPlayer(intent.mediaId, 0L))
                }
            }
        }
    }

    private fun updateChipState(chip: LibraryFilterChip, enabled: Boolean) {
        activeFilterChips = if (enabled) {
            activeFilterChips.add(chip)
        } else {
            activeFilterChips.remove(chip)
        }
    }

    private fun loadInitialLibrary() {
        initialJob = viewModelScope.launch {
            if (currentLibraryId.isNotBlank()) return@launch
            _uiState.value = LibraryUiState.Loading

            // Dynamically discover library folders from server
            val viewsResult = handshakeRepository.getUserViews()
            val libraries = viewsResult.getOrDefault(emptyList())
            cachedLibraries = libraries.toImmutableList()
            val firstLibrary = libraries.firstOrNull()

            if (currentLibraryId.isNotBlank()) return@launch

            if (firstLibrary != null) {
                currentLibraryId = firstLibrary.id
                currentLibraryName = "المكتبة"
                loadItems()
            } else {
                _uiState.value = LibraryUiState.Empty("لا توجد مكتبات وسائط متوفرة على السيرفر")
            }
        }
    }

    private fun loadItems(isRefresh: Boolean = false) {
        if (currentLibraryId.isBlank()) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!isRefresh && _uiState.value !is LibraryUiState.Success) {
                _uiState.value = LibraryUiState.Loading
            } else if (isRefresh && _uiState.value is LibraryUiState.Success) {
                val current = _uiState.value as LibraryUiState.Success
                _uiState.value = current.copy(isRefreshing = true)
            }

            getLibraryItemsUseCase(
                parentId = currentLibraryId,
                filter = currentFilter,
                letter = currentLetter
            ).onSuccess { page ->
                val filteredItems = if (currentFilter.isFavoriteOnly && localProfileRepository != null) {
                    page.items.filter { localProfileRepository.isFavorite(it.id) }.toImmutableList()
                } else {
                    page.items
                }

                if (filteredItems.isEmpty() && activeFilterChips.isEmpty() && currentLetter == null) {
                    _uiState.value = LibraryUiState.Empty("المكتبة فارغة حالياً على الخادم")
                } else {
                    _uiState.value = LibraryUiState.Success(
                        items = filteredItems,
                        totalCount = filteredItems.size,
                        activeFilter = currentFilter,
                        currentLibraryId = currentLibraryId,
                        currentLibraryName = "المكتبة",
                        availableLibraries = cachedLibraries,
                        selectedLibraryId = currentLibraryId,
                        selectedSort = currentFilter.sortBy,
                        activeFilters = activeFilterChips,
                        selectedLetter = currentLetter,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                _uiState.value = LibraryUiState.Error(
                    message = error.localizedMessage ?: "حدث خطأ أثناء تحميل محتوى المكتبة"
                )
            }
        }
    }
}
