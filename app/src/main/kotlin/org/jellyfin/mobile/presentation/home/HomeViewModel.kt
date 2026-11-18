package org.jellyfin.mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.data.mapper.toContinueWatchingItem
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.HomeRepository
import org.jellyfin.mobile.domain.usecase.GetHomeFeedUseCase

/**
 * HomeViewModel implements MVI architecture for the Home Dashboard.
 * Strictly adheres to docs/screens/01_HOME_DASHBOARD.md.
 */
class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val handshakeRepository: HandshakeRepository,
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        processIntent(HomeIntent.LoadDashboardData)
    }

    fun processIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadDashboardData -> loadData(isRefresh = false)
            is HomeIntent.RefreshDashboard -> loadData(isRefresh = true)
            is HomeIntent.SelectHeroIndex -> updateHeroIndex(intent.index)
            is HomeIntent.SelectLibrary -> {
                viewModelScope.launch {
                    _effects.send(HomeEffect.NavigateToLibraryBrowser(intent.libraryId, intent.title))
                }
            }
            is HomeIntent.QuickPlay -> {
                viewModelScope.launch {
                    _effects.send(HomeEffect.LaunchPlayer(intent.mediaId, intent.resumePositionMs))
                }
            }
            is HomeIntent.OpenMediaDetails -> {
                viewModelScope.launch {
                    _effects.send(HomeEffect.NavigateToDetails(intent.mediaId))
                }
            }
            is HomeIntent.DismissFromContinueWatching -> {
                removeContinueWatchingItem(intent.mediaId)
                viewModelScope.launch {
                    homeRepository.dismissContinueWatching(intent.mediaId)
                        .onFailure {
                            loadData(isRefresh = false)
                        }
                }
            }
            is HomeIntent.MarkItemWatched -> {
                viewModelScope.launch {
                    homeRepository.markItemWatched(intent.mediaId)
                        .onSuccess {
                            _effects.send(HomeEffect.ShowToast("تم التحديد كمشاهد ✓"))
                        }
                        .onFailure {
                            _effects.send(HomeEffect.ShowToast("فشل تحديث حالة المشاهدة على السيرفر"))
                        }
                }
            }
        }
    }

    private fun loadData(isRefresh: Boolean) {
        viewModelScope.launch {
            if (!isRefresh && _uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            } else if (isRefresh && _uiState.value is HomeUiState.Success) {
                val current = _uiState.value as HomeUiState.Success
                _uiState.value = current.copy(isRefreshing = true)
            }

            // Ensure server session exists
            if (handshakeRepository.currentSession.value == null) {
                val handshakeResult = handshakeRepository.performHandshake()
                if (handshakeResult.isFailure) {
                    _uiState.value = HomeUiState.Error(
                        message = handshakeResult.exceptionOrNull()?.localizedMessage
                            ?: "فشل الاتصال بخادم Jellyfin",
                        canRetryOffline = true
                    )
                    return@launch
                }
            }

            // Fetch dynamic home feed
            getHomeFeedUseCase(forceRefresh = isRefresh)
                .onSuccess { feed ->
                    val serverInfo = handshakeRepository.serverInfo.value
                    val userSession = handshakeRepository.currentSession.value
                    val displayName = userSession?.userName ?: serverInfo?.name ?: "Jellyfin"

                    val heroList = feed.heroItems.ifEmpty {
                        feed.latestItems.take(5).toImmutableList()
                    }

                    val continueWatching = feed.resumeItems.map { it.toContinueWatchingItem() }.toImmutableList()

                    _uiState.value = HomeUiState.Success(
                        heroItems = heroList,
                        currentHeroIndex = 0,
                        libraries = feed.libraries,
                        continueWatchingItems = continueWatching,
                        recentlyAddedItems = feed.latestItems,
                        userDisplayName = displayName,
                        isRefreshing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(
                        message = error.localizedMessage ?: "حدث خطأ أثناء تحميل بيانات الشاشة الرئيسية",
                        canRetryOffline = true
                    )
                }
        }
    }

    private fun updateHeroIndex(index: Int) {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(currentHeroIndex = index)
        }
    }

    private fun removeContinueWatchingItem(mediaId: String) {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            val updated = current.continueWatchingItems.filterNot { it.id == mediaId }.toImmutableList()
            _uiState.value = current.copy(continueWatchingItems = updated)
        }
    }
}
