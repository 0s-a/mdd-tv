package org.jellyfin.mobile.presentation.details.boxset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.core.player.external.ExternalPlaybackResult
import org.jellyfin.mobile.core.player.external.ExternalPlayerManager
import org.jellyfin.mobile.core.player.external.ExternalPlayerPayload
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LibraryRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository
import org.jellyfin.mobile.domain.usecase.GetMediaDetailsUseCase

/**
 * ViewModel for the BoxSets / Collections details screen.
 * Handles fetching collection details, child movies list, computing cumulative stats,
 * and tracking the next unwatched movie part.
 * Zero-emoji compliance.
 */
class BoxSetViewModel(
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val libraryRepository: LibraryRepository,
    private val mediaDetailsRepository: MediaDetailsRepository,
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository? = null,
    private val apiService: JellyfinApiService? = null,
    private val sessionManager: SessionManager? = null,
    private val externalPlayerManager: ExternalPlayerManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<BoxSetUiState>(BoxSetUiState.Loading)
    val uiState: StateFlow<BoxSetUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BoxSetEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentBoxSetId: String? = null

    fun processIntent(intent: BoxSetIntent) {
        when (intent) {
            is BoxSetIntent.LoadBoxSet -> {
                currentBoxSetId = intent.boxSetId
                loadBoxSet(intent.boxSetId, isRefresh = false)
            }
            is BoxSetIntent.RetryLoad -> {
                currentBoxSetId?.let { loadBoxSet(it, isRefresh = false) }
            }
            is BoxSetIntent.Refresh -> {
                currentBoxSetId?.let { loadBoxSet(it, isRefresh = true) }
            }
            is BoxSetIntent.PlayMovie -> {
                handlePlayMovie(intent.movieId, intent.resumePositionMs)
            }
            is BoxSetIntent.ResumeFranchise -> {
                handleResumeFranchise()
            }
            is BoxSetIntent.PlayFromBeginning -> {
                handlePlayFromBeginning()
            }
            is BoxSetIntent.PlayExternalNext -> {
                handlePlayExternalNext()
            }
            is BoxSetIntent.PlayMovieExternal -> {
                handlePlayMovieExternal(intent.movieId)
            }
            is BoxSetIntent.ToggleFavorite -> {
                toggleFavorite()
            }
            is BoxSetIntent.ToggleWatchlist -> {
                toggleWatchlist()
            }
            is BoxSetIntent.OpenSaveModal -> {
                val current = _uiState.value
                if (current is BoxSetUiState.Success) {
                    _uiState.value = current.copy(isSaveModalVisible = true)
                }
            }
            is BoxSetIntent.DismissSaveModal -> {
                val current = _uiState.value
                if (current is BoxSetUiState.Success) {
                    _uiState.value = current.copy(isSaveModalVisible = false)
                }
            }
            is BoxSetIntent.ToggleMovieWatched -> {
                toggleMovieWatched(intent.movieId)
            }
            is BoxSetIntent.ToggleEntireCollectionWatched -> {
                toggleEntireCollectionWatched()
            }
            is BoxSetIntent.OpenMovieDetails -> {
                viewModelScope.launch {
                    _effects.send(BoxSetEffect.NavigateToMovieDetails(intent.movieId))
                }
            }
            is BoxSetIntent.ShareFranchise -> {
                handleShareFranchise()
            }
        }
    }

    private fun loadBoxSet(boxSetId: String, isRefresh: Boolean) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.value = BoxSetUiState.Loading
            } else {
                val current = _uiState.value
                if (current is BoxSetUiState.Success) {
                    _uiState.value = current.copy(isRefreshing = true)
                }
            }

            try {
                // 1. Fetch collection details
                val detailsDeferred = async {
                    getMediaDetailsUseCase(boxSetId)
                }

                // 2. Fetch collection child movies
                val partsDeferred = async {
                    fetchCollectionParts(boxSetId)
                }

                val detailsResult = detailsDeferred.await()
                val partsList = partsDeferred.await()

                if (detailsResult.isFailure && partsList.isEmpty()) {
                    val errorMsg = detailsResult.exceptionOrNull()?.localizedMessage
                        ?: "فشل تحميل تفاصيل السلسلة"
                    _uiState.value = BoxSetUiState.Error(message = errorMsg)
                    return@launch
                }

                val details = detailsResult.getOrThrow()

                // 3. Compute cumulative stats
                val totalTicks = partsList.sumOf { it.runTimeTicks ?: 0L }
                val formattedCumulativeRuntime = formatCumulativeRuntime(totalTicks)

                // 4. Track next unwatched part
                val (nextPart, nextIndex) = calculateNextUnwatched(partsList)
                val allWatched = partsList.isNotEmpty() && partsList.all { it.isWatched }

                _uiState.value = BoxSetUiState.Success(
                    boxSetDetails = details,
                    parts = partsList.toImmutableList(),
                    totalPartsCount = partsList.size,
                    totalRuntimeTicks = totalTicks,
                    formattedTotalRuntime = formattedCumulativeRuntime,
                    nextUnwatchedPart = nextPart,
                    nextUnwatchedIndex = nextIndex,
                    isFavorite = details.isFavorite,
                    isWatchlist = false,
                    isWatched = allWatched,
                    isSaveModalVisible = false,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = BoxSetUiState.Error(
                    message = e.localizedMessage ?: "حدث خطأ غير متوقع",
                    isNetworkError = true
                )
            }
        }
    }

    private suspend fun fetchCollectionParts(boxSetId: String): List<BoxSetPartItem> {
        val rawItems = try {
            val libraryResult = libraryRepository.getLibraryItems(
                parentId = boxSetId,
                filter = LibraryFilter(),
                isHierarchical = false,
                limit = 200
            )
            libraryResult.getOrNull()?.items ?: emptyList()
        } catch (_: Exception) {
            emptyList<MediaItemSummary>()
        }

        // Chronological sort: by production year ascending, then title
        val sortedMovies = rawItems
            .filter { it.isMovie || !it.isFolder }
            .sortedWith(
                compareBy<MediaItemSummary> { it.productionYear ?: Int.MAX_VALUE }
                    .thenBy { it.name }
            )

        return sortedMovies.mapIndexed { index, movie ->
            BoxSetPartItem(
                id = movie.id,
                title = movie.name,
                partIndex = index + 1,
                productionYear = movie.productionYear,
                communityRating = movie.communityRating,
                runTimeTicks = movie.runTimeTicks,
                durationText = movie.durationText ?: formatDuration(movie.runTimeTicks),
                posterUrl = movie.posterImageUrl,
                backdropUrl = movie.backdropImageUrl,
                isPlayed = movie.isPlayed,
                playbackPositionPercentage = movie.playbackPositionPercentage,
                overview = movie.overview,
                isFavorite = movie.isFavorite
            )
        }
    }

    private fun handlePlayMovie(movieId: String, resumePositionMs: Long) {
        viewModelScope.launch {
            _effects.send(BoxSetEffect.LaunchPlayer(movieId, resumePositionMs))
        }
    }

    private fun handleResumeFranchise() {
        val state = _uiState.value as? BoxSetUiState.Success ?: return
        viewModelScope.launch {
            val target = state.nextUnwatchedPart ?: state.parts.firstOrNull()
            if (target != null) {
                _effects.send(
                    BoxSetEffect.LaunchPlayer(
                        mediaId = target.id,
                        resumePositionMs = target.resumePositionMs
                    )
                )
            } else {
                _effects.send(BoxSetEffect.ShowToast("لا توجد أفلام في هذه السلسلة"))
            }
        }
    }

    private fun handlePlayFromBeginning() {
        val state = _uiState.value as? BoxSetUiState.Success ?: return
        viewModelScope.launch {
            val firstPart = state.parts.firstOrNull()
            if (firstPart != null) {
                _effects.send(
                    BoxSetEffect.LaunchPlayer(
                        mediaId = firstPart.id,
                        resumePositionMs = 0L
                    )
                )
            } else {
                _effects.send(BoxSetEffect.ShowToast("لا توجد أفلام في هذه السلسلة"))
            }
        }
    }

    private fun handlePlayExternalNext() {
        val current = _uiState.value as? BoxSetUiState.Success ?: return
        val targetPart = current.nextUnwatchedPart ?: current.parts.firstOrNull()
        if (targetPart != null) {
            handlePlayMovieExternal(targetPart.id)
        } else {
            viewModelScope.launch {
                _effects.send(BoxSetEffect.ShowToast("لا توجد أجزاء متاحة للتشغيل"))
            }
        }
    }

    private fun handlePlayMovieExternal(movieId: String) {
        val current = _uiState.value as? BoxSetUiState.Success ?: return
        val targetPart = current.parts.find { it.id == movieId } ?: return
        val title = "${current.boxSetDetails.title} - ${targetPart.title}"
        val resumeMs = targetPart.resumePositionMs
        val durationMs = targetPart.runtimeMs

        val token = sessionManager?.getToken() ?: handshakeRepository.currentSession.value?.token
        val baseServer = AppConfig.SERVER_URL.trimEnd('/')
        val streamUrl = StringBuilder("$baseServer/Videos/$movieId/stream?static=true")
        if (!token.isNullOrBlank()) {
            streamUrl.append("&api_key=$token")
        }

        val payload = ExternalPlayerPayload(
            mediaId = movieId,
            title = title,
            streamUrl = streamUrl.toString(),
            resumePositionMs = resumeMs,
            durationMs = durationMs,
            subtitles = emptyList()
        )

        viewModelScope.launch {
            _effects.send(BoxSetEffect.LaunchExternalPlayer(payload))
        }
    }

    fun onExternalPlaybackResult(result: ExternalPlaybackResult, partTitle: String) {
        viewModelScope.launch {
            externalPlayerManager?.persistResult(result, partTitle, "Movie")
            currentBoxSetId?.let { loadBoxSet(it, isRefresh = true) }
        }
    }

    private fun toggleFavorite() {
        val currentState = _uiState.value as? BoxSetUiState.Success ?: return
        val boxSetId = currentBoxSetId ?: return
        val newFavorite = !currentState.isFavorite

        viewModelScope.launch {
            _uiState.value = currentState.copy(isFavorite = newFavorite)
            val result = mediaDetailsRepository.toggleFavorite(boxSetId, newFavorite)
            if (result.isFailure) {
                _uiState.value = currentState
                _effects.send(BoxSetEffect.ShowSnackbar("فشل تحديث المفضلة"))
            } else {
                localProfileRepository?.let { localRepo ->
                    localRepo.toggleFavorite(
                        itemId = boxSetId,
                        title = currentState.boxSetDetails.title,
                        mediaType = "BoxSet",
                        releaseYear = currentState.boxSetDetails.productionYear?.toString().orEmpty(),
                        communityRating = currentState.boxSetDetails.communityRating?.toString().orEmpty()
                    )
                }
            }
        }
    }

    private fun toggleWatchlist() {
        val current = _uiState.value as? BoxSetUiState.Success ?: return
        val targetWatchlist = !current.isWatchlist
        _uiState.value = current.copy(isWatchlist = targetWatchlist)
        viewModelScope.launch {
            val msg = if (targetWatchlist) "تمت إضافة السلسلة إلى قائمتك" else "تمت إزالة السلسلة من قائمتك"
            _effects.send(BoxSetEffect.ShowToast(msg))
        }
    }

    private fun toggleMovieWatched(movieId: String) {
        val currentState = _uiState.value as? BoxSetUiState.Success ?: return
        val targetPart = currentState.parts.find { it.id == movieId } ?: return
        val newWatched = !targetPart.isWatched

        viewModelScope.launch {
            val updatedParts = currentState.parts.map { part ->
                if (part.id == movieId) {
                    part.copy(
                        isPlayed = newWatched,
                        playbackPositionPercentage = if (newWatched) 100.0 else 0.0
                    )
                } else {
                    part
                }
            }.toImmutableList()

            val (nextPart, nextIndex) = calculateNextUnwatched(updatedParts)
            val allWatched = updatedParts.isNotEmpty() && updatedParts.all { it.isWatched }
            _uiState.value = currentState.copy(
                parts = updatedParts,
                nextUnwatchedPart = nextPart,
                nextUnwatchedIndex = nextIndex,
                isWatched = allWatched
            )

            val result = mediaDetailsRepository.markAsWatched(movieId, newWatched)
            if (result.isFailure) {
                _uiState.value = currentState
                _effects.send(BoxSetEffect.ShowSnackbar("فشل تحديث حالة المشاهدة"))
            } else {
                localProfileRepository?.let { localRepo ->
                    if (newWatched) {
                        localRepo.markWatched(movieId)
                    }
                }
            }
        }
    }

    private fun toggleEntireCollectionWatched() {
        val current = _uiState.value as? BoxSetUiState.Success ?: return
        val newWatchedState = !current.isWatched

        val updatedParts = current.parts.map { part ->
            part.copy(
                isPlayed = newWatchedState,
                playbackPositionPercentage = if (newWatchedState) 100.0 else 0.0
            )
        }.toImmutableList()

        val (nextPart, nextIndex) = calculateNextUnwatched(updatedParts)
        _uiState.value = current.copy(
            parts = updatedParts,
            nextUnwatchedPart = nextPart,
            nextUnwatchedIndex = nextIndex,
            isWatched = newWatchedState
        )

        viewModelScope.launch {
            current.parts.forEach { part ->
                mediaDetailsRepository.markAsWatched(part.id, newWatchedState)
                if (newWatchedState) {
                    localProfileRepository?.markWatched(part.id)
                }
            }
            val msg = if (newWatchedState) "تم تحديد السلسلة بالكامل كمشاهدة" else "تم تحديد السلسلة كغير مشاهدة"
            _effects.send(BoxSetEffect.ShowToast(msg))
        }
    }

    private fun handleShareFranchise() {
        val state = _uiState.value as? BoxSetUiState.Success ?: return
        val title = state.boxSetDetails.title
        val count = state.totalPartsCount
        val duration = state.formattedTotalRuntime
        val shareText = "شاهد سلسلة $title ($count أجزاء · $duration) على تطبيق مدد TV"
        viewModelScope.launch {
            _effects.send(BoxSetEffect.ShareText(shareText))
        }
    }

    private fun calculateNextUnwatched(parts: List<BoxSetPartItem>): Pair<BoxSetPartItem?, Int?> {
        val nextUnwatched = parts.firstOrNull { !it.isWatched }
        return nextUnwatched to nextUnwatched?.partIndex
    }

    private fun formatCumulativeRuntime(ticks: Long): String {
        if (ticks <= 0L) return ""
        val totalMinutes = (ticks / 10_000_000L / 60L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "إجمالي $hours ساعات و $minutes د"
            hours > 0 -> "إجمالي $hours ساعات"
            minutes > 0 -> "إجمالي $minutes د"
            else -> ""
        }
    }

    private fun formatDuration(ticks: Long?): String? {
        if (ticks == null || ticks <= 0L) return null
        val totalMinutes = (ticks / 10_000_000L / 60L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}س ${minutes}د"
            hours > 0 -> "${hours}س"
            minutes > 0 -> "${minutes}د"
            else -> null
        }
    }
}
