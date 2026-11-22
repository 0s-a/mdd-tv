package org.jellyfin.mobile.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.BreadcrumbItem
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.LibrarySortBy
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.usecase.GetLibraryItemsUseCase

/**
 * LibraryViewModel manages state and intents for the Pure Hierarchical Library Browser Screen.
 * Strictly adheres to MVI pattern and docs/current/screens/02_library_browser.md.
 * Zero emojis across all code and comments.
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
    private var activeParentId: String = ""
    private var currentLibraryName: String = "المكتبة"
    private var cachedLibraries: ImmutableList<LibraryFolder> = persistentListOf()
    private var breadcrumbTrail = persistentListOf<BreadcrumbItem>()
    private var currentFilter = LibraryFilter()
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
                activeParentId = intent.parentId
                if (intent.libraryName.isNotBlank()) currentLibraryName = intent.libraryName
                breadcrumbTrail = persistentListOf(
                    BreadcrumbItem(id = "", name = "المكتبة", level = 0),
                    BreadcrumbItem(id = intent.parentId, name = currentLibraryName, level = 1)
                )
                ensureCachedLibraries()
                loadItems()
            }
            is LibraryIntent.SwitchLibrary -> {
                initialJob?.cancel()
                currentLibraryId = intent.libraryId
                activeParentId = intent.libraryId
                currentLibraryName = if (intent.libraryName.isNotBlank()) intent.libraryName else "المكتبة"
                breadcrumbTrail = persistentListOf(
                    BreadcrumbItem(id = "", name = "المكتبة", level = 0),
                    BreadcrumbItem(id = intent.libraryId, name = currentLibraryName, level = 1)
                )
                ensureCachedLibraries()
                loadItems()
                sendScrollToTop()
            }
            is LibraryIntent.NavigateIntoFolder -> {
                if (breadcrumbTrail.size <= 1 && (currentLibraryId.isBlank() || breadcrumbTrail.firstOrNull()?.id.isNullOrBlank())) {
                    currentLibraryId = intent.folderId
                    currentLibraryName = intent.folderName
                    activeParentId = intent.folderId
                    breadcrumbTrail = persistentListOf(
                        BreadcrumbItem(id = "", name = "المكتبة", level = 0),
                        BreadcrumbItem(id = intent.folderId, name = intent.folderName, level = 1)
                    )
                } else {
                    activeParentId = intent.folderId
                    breadcrumbTrail = (breadcrumbTrail + BreadcrumbItem(
                        id = intent.folderId,
                        name = intent.folderName,
                        level = breadcrumbTrail.size
                    )).toPersistentList()
                }
                loadItems()
                sendScrollToTop()
            }
            is LibraryIntent.NavigateBackInHierarchy -> {
                if (breadcrumbTrail.size > 2) {
                    breadcrumbTrail = breadcrumbTrail.dropLast(1).toPersistentList()
                    activeParentId = breadcrumbTrail.last().id
                    loadItems()
                    sendScrollToTop()
                } else if (breadcrumbTrail.size == 2) {
                    if (cachedLibraries.isNotEmpty()) {
                        loadRootView(cachedLibraries)
                    } else {
                        loadInitialLibrary()
                    }
                    sendScrollToTop()
                }
            }
            is LibraryIntent.JumpToBreadcrumb -> {
                if (intent.targetIndex == 0) {
                    if (cachedLibraries.isNotEmpty()) {
                        loadRootView(cachedLibraries)
                    } else {
                        loadInitialLibrary()
                    }
                    sendScrollToTop()
                } else if (intent.targetIndex in 1 until breadcrumbTrail.size) {
                    breadcrumbTrail = breadcrumbTrail.take(intent.targetIndex + 1).toPersistentList()
                    activeParentId = breadcrumbTrail.last().id
                    if (breadcrumbTrail.size >= 2) {
                        currentLibraryId = breadcrumbTrail[1].id
                        currentLibraryName = breadcrumbTrail[1].name
                    }
                    loadItems()
                    sendScrollToTop()
                }
            }
            is LibraryIntent.ChangeSort -> {
                currentFilter = currentFilter.copy(sortBy = intent.sortBy)
                loadItems()
                sendScrollToTop()
            }
            is LibraryIntent.Refresh -> {
                if (breadcrumbTrail.size <= 1) {
                    initialJob?.cancel()
                    currentLibraryId = ""
                    loadInitialLibrary()
                } else {
                    loadItems(isRefresh = true)
                }
            }
            is LibraryIntent.LoadNextPage -> {
                loadNextPage()
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
        }
    }

    private fun ensureCachedLibraries() {
        if (cachedLibraries.isEmpty()) {
            viewModelScope.launch {
                val viewsResult = handshakeRepository.getUserViews()
                cachedLibraries = viewsResult.getOrDefault(emptyList()).toImmutableList()
            }
        }
    }

    private fun sendScrollToTop() {
        viewModelScope.launch {
            _effects.send(LibraryEffect.ScrollToIndex(0))
        }
    }

    private fun loadInitialLibrary() {
        initialJob = viewModelScope.launch {
            if (currentLibraryId.isNotBlank()) return@launch
            _uiState.value = LibraryUiState.Loading

            // Discover library folders dynamically from server views
            val viewsResult = handshakeRepository.getUserViews()
            val libraries = viewsResult.getOrDefault(emptyList())
            cachedLibraries = libraries.toImmutableList()

            if (currentLibraryId.isNotBlank()) return@launch

            if (libraries.isNotEmpty()) {
                loadRootView(libraries)
            } else {
                _uiState.value = LibraryUiState.Empty("لا توجد مكتبات وسائط متوفرة على السيرفر")
            }
        }
    }

    private fun loadRootView(libraries: List<LibraryFolder>) {
        currentLibraryId = ""
        activeParentId = ""
        currentLibraryName = "المكتبة"
        breadcrumbTrail = persistentListOf(
            BreadcrumbItem(id = "", name = "المكتبة", level = 0)
        )
        val folderItems = libraries.map { lib ->
            MediaItemSummary(
                id = lib.id,
                name = lib.name,
                type = "Folder",
                posterImageUrl = JellyfinApiService.buildPrimaryImageUrl(lib.id)
            )
        }.toImmutableList()

        _uiState.value = LibraryUiState.Success(
            items = folderItems,
            totalCount = folderItems.size,
            currentLibraryId = "",
            currentLibraryName = "المكتبة",
            availableLibraries = cachedLibraries,
            selectedLibraryId = null,
            selectedSort = currentFilter.sortBy,
            isRefreshing = false,
            isLoadingMore = false,
            hasMoreItems = false,
            breadcrumbs = breadcrumbTrail,
            isRoot = true
        )
    }

    private fun loadItems(isRefresh: Boolean = false) {
        if (currentLibraryId.isBlank()) {
            if (cachedLibraries.isNotEmpty()) {
                loadRootView(cachedLibraries)
            } else {
                loadInitialLibrary()
            }
            return
        }

        val queryParentId = activeParentId.ifBlank { currentLibraryId }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!isRefresh && _uiState.value !is LibraryUiState.Success) {
                _uiState.value = LibraryUiState.Loading
            } else if (isRefresh && _uiState.value is LibraryUiState.Success) {
                val current = _uiState.value as LibraryUiState.Success
                _uiState.value = current.copy(isRefreshing = true)
            }

            getLibraryItemsUseCase(
                parentId = queryParentId,
                filter = currentFilter,
                letter = null,
                startIndex = 0,
                limit = 60,
                isHierarchical = true,
                forceRefresh = isRefresh
            ).onSuccess { page ->
                _uiState.value = LibraryUiState.Success(
                    items = page.items,
                    totalCount = page.totalCount,
                    currentLibraryId = currentLibraryId,
                    currentLibraryName = currentLibraryName,
                    availableLibraries = cachedLibraries,
                    selectedLibraryId = currentLibraryId,
                    selectedSort = currentFilter.sortBy,
                    isRefreshing = false,
                    isLoadingMore = false,
                    hasMoreItems = page.items.size < page.totalCount,
                    breadcrumbs = breadcrumbTrail,
                    isRoot = false
                )
            }.onFailure { error ->
                _uiState.value = LibraryUiState.Error(
                    message = error.localizedMessage ?: "حدث خطأ أثناء تحميل محتوى المكتبة"
                )
            }
        }
    }

    private var isPaginating = false

    private fun loadNextPage() {
        val currentState = _uiState.value as? LibraryUiState.Success ?: return
        if (currentState.isAtRoot) return
        if (isPaginating || !currentState.hasMoreItems || currentState.isLoadingMore) return
        if (currentLibraryId.isBlank()) return

        val queryParentId = activeParentId.ifBlank { currentLibraryId }
        val currentItems = currentState.items
        val startIndex = currentItems.size

        isPaginating = true
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            getLibraryItemsUseCase(
                parentId = queryParentId,
                filter = currentFilter,
                letter = null,
                startIndex = startIndex,
                limit = 60,
                isHierarchical = true
            ).onSuccess { page ->
                val existingIds = currentItems.map { it.id }.toSet()
                val distinctNew = page.items.filter { it.id !in existingIds }
                val combined = (currentItems + distinctNew).toImmutableList()

                _uiState.value = currentState.copy(
                    items = combined,
                    totalCount = page.totalCount,
                    isLoadingMore = false,
                    hasMoreItems = distinctNew.isNotEmpty() && combined.size < page.totalCount,
                    breadcrumbs = breadcrumbTrail
                )
            }.onFailure {
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
            isPaginating = false
        }
    }
}
