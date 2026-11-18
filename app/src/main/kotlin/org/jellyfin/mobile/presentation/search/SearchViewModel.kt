package org.jellyfin.mobile.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.SearchRepository
import org.jellyfin.mobile.domain.usecase.SearchMediaUseCase

/**
 * SearchViewModel manages reactive universal search with zero race conditions.
 * Uses Coroutines debounce(300ms) -> distinctUntilChanged() -> flatMapLatest().
 * Strictly adheres to docs/screens/05_SEARCH_AND_EXPLORE.md.
 */
class SearchViewModel(
    private val searchMediaUseCase: SearchMediaUseCase,
    private val searchRepository: SearchRepository,
    private val handshakeRepository: HandshakeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = _searchQuery.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(
        SearchUiState.Idle(
            recentQueries = emptyList<String>().toImmutableList(),
            genres = searchRepository.getExploreGenres().toImmutableList()
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadInitialIdleState()
        setupSearchPipeline()
    }

    private fun loadInitialIdleState() {
        viewModelScope.launch {
            val recent = searchRepository.getRecentQueries().first().toImmutableList()
            val genres = searchRepository.getExploreGenres().toImmutableList()
            if (_searchQuery.value.isBlank()) {
                _uiState.value = SearchUiState.Idle(recentQueries = recent, genres = genres)
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun setupSearchPipeline() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { queryText ->
                    flow {
                        val trimmed = queryText.trim()
                        if (trimmed.isBlank()) {
                            val recent = searchRepository.getRecentQueries().first().toImmutableList()
                            val genres = searchRepository.getExploreGenres().toImmutableList()
                            emit(SearchUiState.Idle(recentQueries = recent, genres = genres))
                        } else {
                            emit(SearchUiState.Searching)

                            // Ensure active session
                            if (handshakeRepository.currentSession.value == null) {
                                handshakeRepository.performHandshake()
                            }

                            searchMediaUseCase(trimmed).fold(
                                onSuccess = { result ->
                                    if (result.topMatch == null && result.mediaResults.isEmpty() && result.peopleResults.isEmpty()) {
                                        emit(SearchUiState.Empty(query = trimmed))
                                    } else {
                                        searchRepository.addRecentQuery(trimmed)
                                        emit(
                                            SearchUiState.Success(
                                                query = trimmed,
                                                topMatch = result.topMatch,
                                                mediaResults = result.mediaResults,
                                                peopleResults = result.peopleResults,
                                                totalCount = result.totalCount
                                            )
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    emit(SearchUiState.Error(message = error.localizedMessage ?: "حدث خطأ أثناء البحث"))
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
                searchByGenre(intent.genre)
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
        }
    }

    private fun searchByGenre(genreName: String) {
        _searchQuery.value = genreName
        viewModelScope.launch {
            _uiState.value = SearchUiState.Searching
            searchRepository.getItemsByGenre(genreName)
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        _uiState.value = SearchUiState.Empty(genreName)
                    } else {
                        val top = items.firstOrNull()
                        val rest = if (top != null) items.filterNot { it.id == top.id } else items
                        _uiState.value = SearchUiState.Success(
                            query = genreName,
                            topMatch = top,
                            mediaResults = rest.toImmutableList(),
                            peopleResults = emptyList<CastMember>().toImmutableList(),
                            totalCount = items.size
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = SearchUiState.Error(error.localizedMessage ?: "تعذر استعراض تصنيف $genreName")
                }
        }
    }

    private suspend fun refreshIdleStateIfBlank() {
        if (_searchQuery.value.isBlank()) {
            val recent = searchRepository.getRecentQueries().first().toImmutableList()
            val genres = searchRepository.getExploreGenres().toImmutableList()
            _uiState.value = SearchUiState.Idle(recentQueries = recent, genres = genres)
        }
    }
}
