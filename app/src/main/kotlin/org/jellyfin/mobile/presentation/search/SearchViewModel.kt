package org.jellyfin.mobile.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.SearchRepository
import org.jellyfin.mobile.domain.usecase.SearchMediaUseCase

/**
 * SearchViewModel manages reactive universal search with zero race conditions.
 * Uses combine(searchQuery, selectedGenres) -> debounce(300ms) -> distinctUntilChanged() -> flatMapLatest().
 * 100% dynamic server data for available genres, random spotlight, and recent search history.
 */
class SearchViewModel(
    private val searchMediaUseCase: SearchMediaUseCase,
    private val searchRepository: SearchRepository,
    private val handshakeRepository: HandshakeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = _searchQuery.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    val selectedGenres: StateFlow<Set<String>> = _selectedGenres.asStateFlow()

    private val _availableGenres = MutableStateFlow<List<String>>(emptyList())
    val availableGenres: StateFlow<List<String>> = _availableGenres.asStateFlow()

    private val _spotlightItem = MutableStateFlow<MediaItemSummary?>(null)
    val spotlightItem: StateFlow<MediaItemSummary?> = _spotlightItem.asStateFlow()

    private val _isGenrePickerOpen = MutableStateFlow(false)
    val isGenrePickerOpen: StateFlow<Boolean> = _isGenrePickerOpen.asStateFlow()

    private val _retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _uiState = MutableStateFlow<SearchUiState>(
        SearchUiState.Idle(
            recentQueries = persistentListOf(),
            availableGenres = persistentListOf(),
            selectedGenres = persistentSetOf(),
            spotlightItem = null,
            isGenrePickerOpen = false
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadDynamicServerData()
        setupSearchPipeline()
    }

    private fun loadDynamicServerData() {
        viewModelScope.launch {
            // 1. Fetch available server genres dynamically
            searchRepository.getAvailableGenres().onSuccess { genres ->
                _availableGenres.value = genres
                refreshIdleStateIfBlank()
            }
        }

        viewModelScope.launch {
            // 2. Fetch random dynamic spotlight from server
            searchRepository.getRandomSpotlight().onSuccess { item ->
                _spotlightItem.value = item
                refreshIdleStateIfBlank()
            }
        }

        viewModelScope.launch {
            // 3. Load user's real recent queries
            refreshIdleStateIfBlank()
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun setupSearchPipeline() {
        viewModelScope.launch {
            val searchFlow = combine(_searchQuery, _selectedGenres) { q, g -> Pair(q, g) }
                .debounce(300L)
                .distinctUntilChanged()

            val retryFlow = _retryTrigger.map { Pair(_searchQuery.value, _selectedGenres.value) }

            merge(searchFlow, retryFlow)
                .flatMapLatest { (queryText, genres) ->
                    flow {
                        val trimmed = queryText.trim()
                        if (trimmed.isBlank() && genres.isEmpty()) {
                            val recent = searchRepository.getRecentQueries().first().toImmutableList()
                            emit(
                                SearchUiState.Idle(
                                    recentQueries = recent,
                                    availableGenres = _availableGenres.value.toImmutableList(),
                                    selectedGenres = persistentSetOf(),
                                    spotlightItem = _spotlightItem.value,
                                    isGenrePickerOpen = _isGenrePickerOpen.value
                                )
                            )
                        } else {
                            emit(
                                SearchUiState.Searching(
                                    query = trimmed,
                                    selectedGenres = genres.toImmutableSet(),
                                    isGenrePickerOpen = _isGenrePickerOpen.value
                                )
                            )

                            // Ensure active session
                            if (handshakeRepository.currentSession.value == null) {
                                handshakeRepository.performHandshake()
                            }

                            searchMediaUseCase(trimmed, genres).fold(
                                onSuccess = { result ->
                                    if (result.topMatch == null && result.mediaResults.isEmpty() && result.peopleResults.isEmpty()) {
                                        emit(
                                            SearchUiState.Empty(
                                                query = trimmed,
                                                selectedGenres = genres.toImmutableSet(),
                                                isGenrePickerOpen = _isGenrePickerOpen.value
                                            )
                                        )
                                    } else {
                                        if (trimmed.isNotBlank()) {
                                            searchRepository.addRecentQuery(trimmed)
                                        }
                                        emit(
                                            SearchUiState.Success(
                                                query = trimmed,
                                                selectedGenres = genres.toImmutableSet(),
                                                topMatch = result.topMatch,
                                                mediaResults = result.mediaResults,
                                                peopleResults = result.peopleResults,
                                                totalCount = result.totalCount,
                                                availableGenres = _availableGenres.value.toImmutableList(),
                                                isGenrePickerOpen = _isGenrePickerOpen.value
                                            )
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    emit(
                                        SearchUiState.Error(
                                            message = error.localizedMessage ?: "حدث خطأ أثناء البحث",
                                            selectedGenres = genres.toImmutableSet(),
                                            isGenrePickerOpen = _isGenrePickerOpen.value
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun processIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> {
                _searchQuery.value = intent.query
            }
            is SearchIntent.ClearQuery -> {
                _searchQuery.value = ""
            }
            is SearchIntent.Retry -> {
                _retryTrigger.tryEmit(Unit)
            }
            is SearchIntent.ToggleGenre -> {
                val current = _selectedGenres.value
                _selectedGenres.value = if (current.contains(intent.genre)) {
                    current - intent.genre
                } else {
                    current + intent.genre
                }
            }
            is SearchIntent.RemoveGenre -> {
                _selectedGenres.value = _selectedGenres.value - intent.genre
            }
            is SearchIntent.ClearGenres -> {
                _selectedGenres.value = emptySet()
            }
            is SearchIntent.OpenGenrePicker -> {
                _isGenrePickerOpen.value = true
                updatePickerStateInUiState(true)
            }
            is SearchIntent.CloseGenrePicker -> {
                _isGenrePickerOpen.value = false
                updatePickerStateInUiState(false)
            }
            is SearchIntent.SelectRecentQuery -> {
                _searchQuery.value = intent.query
            }
            is SearchIntent.DeleteRecentQuery -> {
                viewModelScope.launch {
                    searchRepository.removeRecentQuery(intent.query)
                    refreshIdleStateIfBlank()
                }
            }
            is SearchIntent.ClearHistory, is SearchIntent.ClearSearchHistory -> {
                viewModelScope.launch {
                    searchRepository.clearRecentQueries()
                    refreshIdleStateIfBlank()
                }
            }
            is SearchIntent.SelectGenre -> {
                _selectedGenres.value = setOf(intent.genre)
            }
            is SearchIntent.QuickPlay -> {
                viewModelScope.launch {
                    _effects.send(SearchEffect.LaunchPlayer(intent.mediaId))
                }
            }
            is SearchIntent.OpenDetails, is SearchIntent.OpenMediaDetails -> {
                val id = if (intent is SearchIntent.OpenDetails) intent.mediaId else (intent as SearchIntent.OpenMediaDetails).mediaId
                viewModelScope.launch {
                    _effects.send(SearchEffect.NavigateToDetails(id))
                }
            }
            is SearchIntent.OpenPersonProfile -> {
                viewModelScope.launch {
                    _effects.send(SearchEffect.NavigateToPersonFilmography(intent.personId, intent.name))
                }
            }
            is SearchIntent.DismissSearch -> {
                viewModelScope.launch {
                    _effects.send(SearchEffect.Dismiss)
                }
            }
        }
    }

    private fun updatePickerStateInUiState(isOpen: Boolean) {
        _uiState.value = when (val current = _uiState.value) {
            is SearchUiState.Idle -> current.copy(isGenrePickerOpen = isOpen)
            is SearchUiState.Searching -> current.copy(isGenrePickerOpen = isOpen)
            is SearchUiState.Success -> current.copy(isGenrePickerOpen = isOpen)
            is SearchUiState.Empty -> current.copy(isGenrePickerOpen = isOpen)
            is SearchUiState.Error -> current.copy(isGenrePickerOpen = isOpen)
        }
    }

    private suspend fun refreshIdleStateIfBlank() {
        if (_searchQuery.value.isBlank() && _selectedGenres.value.isEmpty()) {
            val recent = searchRepository.getRecentQueries().first().toImmutableList()
            _uiState.value = SearchUiState.Idle(
                recentQueries = recent,
                availableGenres = _availableGenres.value.toImmutableList(),
                selectedGenres = _selectedGenres.value.toImmutableSet(),
                spotlightItem = _spotlightItem.value,
                isGenrePickerOpen = _isGenrePickerOpen.value
            )
        }
    }
}
