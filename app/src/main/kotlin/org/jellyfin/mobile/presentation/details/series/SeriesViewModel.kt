package org.jellyfin.mobile.presentation.details.series

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.core.player.external.ExternalPlayerManager
import org.jellyfin.mobile.core.player.external.ExternalPlayerPayload
import org.jellyfin.mobile.core.player.external.ExternalPlaybackResult
import org.jellyfin.mobile.core.player.preload.PreloadManager
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository
import org.jellyfin.mobile.domain.usecase.GetMediaDetailsUseCase

/**
 * SeriesViewModel coordinates state, episodes loading, next-up resolution,
 * and playback transitions for the dedicated TV Series Details screen.
 */
class SeriesViewModel(
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val mediaDetailsRepository: MediaDetailsRepository,
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository? = null,
    private val apiService: JellyfinApiService? = null,
    private val sessionManager: SessionManager? = null,
    private val preloadManager: PreloadManager? = null,
    private val externalPlayerManager: ExternalPlayerManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<SeriesUiState>(SeriesUiState.Loading)
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SeriesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentSeriesId: String? = null

    fun processIntent(intent: SeriesIntent) {
        when (intent) {
            is SeriesIntent.LoadSeries -> {
                currentSeriesId = intent.seriesId
                loadSeries(intent.seriesId)
            }
            is SeriesIntent.RetryLoad -> {
                currentSeriesId?.let { loadSeries(it) }
            }
            is SeriesIntent.SelectSeason -> {
                selectSeasonByNumber(intent.seasonNumber)
            }
            is SeriesIntent.SelectSeasonById -> {
                selectSeasonById(intent.seasonId)
            }
            is SeriesIntent.PlayEpisode -> {
                handlePlayEpisode(intent.episodeId)
            }
            is SeriesIntent.PlayEpisodeExternal -> {
                handlePlayEpisodeExternal(intent.episodeId)
            }
            is SeriesIntent.ResumeNextUp -> {
                handleResumeNextUp()
            }
            is SeriesIntent.PlayNextUpExternal -> {
                handlePlayNextUpExternal()
            }
            is SeriesIntent.PlayFromStart -> {
                handlePlayFromStart()
            }
            is SeriesIntent.ToggleEpisodeWatched -> {
                toggleEpisodeWatched(intent.episodeId)
            }
            is SeriesIntent.ToggleSeriesFavorite -> {
                toggleSeriesFavorite()
            }
            is SeriesIntent.ToggleSeriesWatchlist -> {
                toggleSeriesWatchlist()
            }
            is SeriesIntent.OpenSaveModal -> {
                val current = _uiState.value
                if (current is SeriesUiState.Success) {
                    _uiState.value = current.copy(isSaveModalVisible = true)
                }
            }
            is SeriesIntent.DismissSaveModal -> {
                val current = _uiState.value
                if (current is SeriesUiState.Success) {
                    _uiState.value = current.copy(isSaveModalVisible = false)
                }
            }
            is SeriesIntent.ToggleSeriesWatched -> {
                toggleSeriesWatched()
            }
            is SeriesIntent.OpenActorFilmography -> {
                viewModelScope.launch {
                    _effects.send(
                        SeriesEffect.NavigateToActorFilmography(
                            personId = intent.personId,
                            personName = intent.personName
                        )
                    )
                }
            }
            is SeriesIntent.RefreshPlaybackState -> {
                refreshPlaybackState()
            }
            is SeriesIntent.ShareSeries -> {
                val current = _uiState.value
                if (current is SeriesUiState.Success) {
                    viewModelScope.launch {
                        _effects.send(
                            SeriesEffect.ShareSeriesContent(
                                title = current.seriesDetails.title,
                                summary = current.seriesDetails.overview
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadSeries(seriesId: String) {
        viewModelScope.launch {
            _uiState.value = SeriesUiState.Loading

            // Verify active server session
            if (handshakeRepository.currentSession.value == null) {
                val handshakeResult = handshakeRepository.performHandshake()
                if (handshakeResult.isFailure) {
                    _uiState.value = SeriesUiState.Error(
                        message = handshakeResult.exceptionOrNull()?.localizedMessage
                            ?: "فشل الاتصال بخادم Jellyfin",
                        isNetworkError = true
                    )
                    return@launch
                }
            }

            getMediaDetailsUseCase(seriesId)
                .onSuccess { details ->
                    val seasons = details.seasons
                    val defaultSeason = seasons.firstOrNull()
                    val defaultSeasonNum = defaultSeason?.indexNumber ?: 1
                    val defaultSeasonId = defaultSeason?.id

                    val episodesForSeason = if (defaultSeasonId != null) {
                        details.episodes.filter { it.seasonId == defaultSeasonId }.ifEmpty { details.episodes }
                    } else {
                        details.episodes
                    }

                    // Next Up Episode resolution: prioritize first unwatched episode
                    val nextUp = details.episodes.firstOrNull { !it.isWatched }
                        ?: episodesForSeason.firstOrNull { !it.isWatched }
                        ?: details.episodes.firstOrNull()

                    val isFav = localProfileRepository?.isFavorite(seriesId) ?: details.isFavorite
                    val isWat = localProfileRepository?.isWatched(seriesId)
                        ?: (details.playbackPositionPercentage >= 95f)

                    _uiState.value = SeriesUiState.Success(
                        seriesDetails = details,
                        nextUpEpisode = nextUp,
                        seasons = seasons,
                        selectedSeasonNumber = defaultSeasonNum,
                        selectedSeasonId = defaultSeasonId,
                        episodesForSelectedSeason = episodesForSeason.toImmutableList(),
                        castList = details.castMembers,
                        isFavorite = isFav,
                        isWatched = isWat,
                        isLoadingEpisodes = false
                    )
                    nextUp?.let { schedulePreload(it.id) }
                }
                .onFailure { error ->
                    _uiState.value = SeriesUiState.Error(
                        message = error.localizedMessage ?: "تعذر تحميل تفاصيل المسلسل",
                        isNetworkError = true
                    )
                }
        }
    }

    private fun selectSeasonByNumber(seasonNumber: Int) {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val targetSeason = current.seasons.find { it.indexNumber == seasonNumber }
        if (targetSeason != null) {
            selectSeasonById(targetSeason.id, seasonNumber)
        } else {
            _uiState.value = current.copy(selectedSeasonNumber = seasonNumber)
        }
    }

    private fun selectSeasonById(seasonId: String, seasonNumber: Int? = null) {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val seriesId = currentSeriesId ?: return
        val effectiveSeasonNum = seasonNumber
            ?: current.seasons.find { it.id == seasonId }?.indexNumber
            ?: 1

        viewModelScope.launch {
            // First check if season episodes are cached
            val cachedEpisodes = current.seriesDetails.episodes.filter { it.seasonId == seasonId }
            if (cachedEpisodes.isNotEmpty()) {
                _uiState.value = current.copy(
                    selectedSeasonId = seasonId,
                    selectedSeasonNumber = effectiveSeasonNum,
                    episodesForSelectedSeason = cachedEpisodes.toImmutableList(),
                    isLoadingEpisodes = false
                )
                return@launch
            }

            // Fetch episodes dynamically from repository
            _uiState.value = current.copy(
                selectedSeasonId = seasonId,
                selectedSeasonNumber = effectiveSeasonNum,
                isLoadingEpisodes = true
            )

            mediaDetailsRepository.getEpisodesForSeason(seriesId = seriesId, seasonId = seasonId)
                .onSuccess { episodes ->
                    _uiState.value = current.copy(
                        selectedSeasonId = seasonId,
                        selectedSeasonNumber = effectiveSeasonNum,
                        episodesForSelectedSeason = episodes.toImmutableList(),
                        isLoadingEpisodes = false
                    )
                }
                .onFailure {
                    _uiState.value = current.copy(isLoadingEpisodes = false)
                    _effects.send(SeriesEffect.ShowToast("تعذر تحميل حلقات هذا الموسم"))
                }
        }
    }

    private fun toggleEpisodeWatched(episodeId: String) {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val episode = current.episodesForSelectedSeason.find { it.id == episodeId }
            ?: current.seriesDetails.episodes.find { it.id == episodeId }
            ?: return

        val targetWatched = !episode.isWatched

        val updatedEpisodes = current.episodesForSelectedSeason.map {
            if (it.id == episodeId) it.copy(isPlayed = targetWatched) else it
        }.toImmutableList()

        // Re-resolve next up episode
        val updatedAllEpisodes = current.seriesDetails.episodes.map {
            if (it.id == episodeId) it.copy(isPlayed = targetWatched) else it
        }
        val newNextUp = updatedAllEpisodes.firstOrNull { !it.isWatched }
            ?: updatedEpisodes.firstOrNull { !it.isWatched }

        _uiState.value = current.copy(
            episodesForSelectedSeason = updatedEpisodes,
            nextUpEpisode = newNextUp
        )

        viewModelScope.launch {
            mediaDetailsRepository.markAsWatched(episodeId, targetWatched)
                .onSuccess {
                    val msg = if (targetWatched) "تم تحديد الحلقة كمشاهدة" else "تمت إزالة علامة المشاهدة"
                    _effects.send(SeriesEffect.ShowToast(msg))
                }
                .onFailure {
                    // Revert state on error
                    val reverted = current.episodesForSelectedSeason.map {
                        if (it.id == episodeId) it.copy(isPlayed = !targetWatched) else it
                    }.toImmutableList()
                    _uiState.value = current.copy(
                        episodesForSelectedSeason = reverted,
                        nextUpEpisode = current.nextUpEpisode
                    )
                    _effects.send(SeriesEffect.ShowToast("فشل تحديث حالة مشاهدة الحلقة"))
                }
        }
    }

    private fun toggleSeriesWatched() {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return
        val seriesId = currentSeriesId ?: return
        val targetWatched = !current.isWatched

        _uiState.value = current.copy(isWatched = targetWatched)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                if (targetWatched) {
                    localProfileRepository.addToArchive(
                        itemId = seriesId,
                        title = current.seriesDetails.title,
                        mediaType = "Series"
                    )
                    localProfileRepository.markWatched(seriesId)
                } else {
                    localProfileRepository.removeFromArchive(seriesId)
                }
            } else {
                mediaDetailsRepository.markAsWatched(seriesId, targetWatched)
            }
            val msg = if (targetWatched) "تم تحديد المسلسل كمشاهد" else "تمت إزالة علامة المشاهدة عن المسلسل"
            _effects.send(SeriesEffect.ShowToast(msg))
        }
    }

    private fun toggleSeriesFavorite() {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return
        val seriesId = currentSeriesId ?: return
        val targetFavorite = !current.isFavorite

        _uiState.value = current.copy(isFavorite = targetFavorite)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                localProfileRepository.toggleFavorite(
                    itemId = seriesId,
                    title = current.seriesDetails.title,
                    mediaType = "Series",
                    releaseYear = current.seriesDetails.productionYear?.toString().orEmpty(),
                    communityRating = current.seriesDetails.communityRating?.toString().orEmpty()
                )
            } else {
                mediaDetailsRepository.toggleFavorite(seriesId, targetFavorite)
            }
            val msg = if (targetFavorite) "تمت إضافة المسلسل إلى المفضلة" else "تمت إزالة المسلسل من المفضلة"
            _effects.send(SeriesEffect.ShowToast(msg))
        }
    }

    private fun toggleSeriesWatchlist() {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return
        val targetWatchlist = !current.isWatchlist
        _uiState.value = current.copy(isWatchlist = targetWatchlist)
        viewModelScope.launch {
            val msg = if (targetWatchlist) "تمت إضافة المسلسل إلى قائمتك" else "تمت إزالة المسلسل من قائمتك"
            _effects.send(SeriesEffect.ShowToast(msg))
        }
    }

    private var lastPlayClickTimeMs: Long = 0L

    private fun handlePlayEpisode(episodeId: String) {
        val now = System.currentTimeMillis()
        if (now - lastPlayClickTimeMs < 800L) return
        lastPlayClickTimeMs = now
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val episode = current.episodesForSelectedSeason.find { it.id == episodeId }
            ?: current.seriesDetails.episodes.find { it.id == episodeId }

        val resumeTicks = if (episode != null && episode.playbackPositionPercentage > 0f) {
            val totalTicks = episode.runTimeTicks ?: 0L
            (totalTicks * (episode.playbackPositionPercentage / 100.0)).toLong()
        } else {
            0L
        }

        val resumeMs = resumeTicks / 10_000L

        viewModelScope.launch {
            _effects.send(SeriesEffect.LaunchPlayer(mediaId = episodeId, resumePositionMs = resumeMs))
        }
    }

    private fun handleResumeNextUp() {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val targetEpisode = current.nextUpEpisode
            ?: current.episodesForSelectedSeason.firstOrNull { !it.isWatched }
            ?: current.seriesDetails.episodes.firstOrNull { !it.isWatched }
            ?: current.episodesForSelectedSeason.firstOrNull()
            ?: current.seriesDetails.episodes.firstOrNull()

        if (targetEpisode != null) {
            handlePlayEpisode(targetEpisode.id)
        } else {
            viewModelScope.launch {
                _effects.send(SeriesEffect.ShowToast("لا توجد حلقات متاحة للتشغيل"))
            }
        }
    }

    private fun handlePlayEpisodeExternal(episodeId: String) {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val episode = current.episodesForSelectedSeason.find { it.id == episodeId }
            ?: current.seriesDetails.episodes.find { it.id == episodeId }
            ?: current.nextUpEpisode

        val title = if (episode != null) {
            "${current.seriesDetails.title} - ${episode.name}"
        } else {
            current.seriesDetails.title
        }

        viewModelScope.launch {
            val resumeTicks = localProfileRepository?.getProgress(episodeId)?.positionTicks ?: 0L
            val resumeMs = resumeTicks / 10_000L

            val token = sessionManager?.getToken() ?: handshakeRepository.currentSession.value?.token
            val baseServer = AppConfig.SERVER_URL.trimEnd('/')
            val streamUrl = StringBuilder("$baseServer/Videos/$episodeId/stream?static=true")
            if (!token.isNullOrBlank()) {
                streamUrl.append("&api_key=$token")
            }

            val payload = ExternalPlayerPayload(
                mediaId = episodeId,
                title = title,
                streamUrl = streamUrl.toString(),
                resumePositionMs = resumeMs,
                durationMs = 0L
            )

            _effects.send(SeriesEffect.LaunchExternalPlayer(payload))
        }
    }

    private fun handlePlayNextUpExternal() {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val targetEpisode = current.nextUpEpisode
            ?: current.episodesForSelectedSeason.firstOrNull { !it.isWatched }
            ?: current.seriesDetails.episodes.firstOrNull { !it.isWatched }
            ?: current.episodesForSelectedSeason.firstOrNull()
            ?: current.seriesDetails.episodes.firstOrNull()

        if (targetEpisode != null) {
            handlePlayEpisodeExternal(targetEpisode.id)
        } else {
            viewModelScope.launch {
                _effects.send(SeriesEffect.ShowToast("لا توجد حلقات متاحة للتشغيل"))
            }
        }
    }

    fun onExternalPlaybackResult(result: ExternalPlaybackResult, episodeTitle: String) {
        viewModelScope.launch {
            externalPlayerManager?.persistResult(result, episodeTitle, "Episode")
            refreshPlaybackState()
        }
    }

    private fun handlePlayFromStart() {
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        val firstEpisode = current.seriesDetails.episodes.firstOrNull()
            ?: current.episodesForSelectedSeason.firstOrNull()

        if (firstEpisode != null) {
            viewModelScope.launch {
                _effects.send(SeriesEffect.LaunchPlayer(mediaId = firstEpisode.id, resumePositionMs = 0L))
            }
        } else {
            viewModelScope.launch {
                _effects.send(SeriesEffect.ShowToast("لا توجد حلقات متاحة للتشغيل"))
            }
        }
    }

    private fun refreshPlaybackState() {
        val seriesId = currentSeriesId ?: return
        val current = _uiState.value
        if (current !is SeriesUiState.Success) return

        viewModelScope.launch {
            getMediaDetailsUseCase(seriesId).onSuccess { details ->
                val isFav = localProfileRepository?.isFavorite(seriesId) ?: details.isFavorite
                val isWat = localProfileRepository?.isWatched(seriesId)
                    ?: (details.playbackPositionPercentage >= 95f)

                val updatedSeasonEpisodes = details.episodes
                    .filter { ep -> current.selectedSeasonId == null || ep.seasonId == current.selectedSeasonId }
                    .ifEmpty { details.episodes }

                val nextUp = details.episodes.firstOrNull { !it.isWatched }
                    ?: updatedSeasonEpisodes.firstOrNull { !it.isWatched }
                    ?: details.episodes.firstOrNull()

                _uiState.value = current.copy(
                    seriesDetails = details,
                    nextUpEpisode = nextUp,
                    isWatched = isWat,
                    isFavorite = isFav,
                    episodesForSelectedSeason = updatedSeasonEpisodes.toImmutableList()
                )
                nextUp?.let { schedulePreload(it.id) }
            }
        }
    }

    private var preloadJob: Job? = null
    private var preloadedEpisodeId: String? = null

    private fun schedulePreload(episodeId: String) {
        preloadJob?.cancel()
        preloadedEpisodeId?.let { oldId ->
            if (oldId != episodeId) {
                preloadManager?.cancelPreload(oldId)
            }
        }
        preloadedEpisodeId = episodeId
        val manager = preloadManager ?: return
        preloadJob = viewModelScope.launch {
            delay(800L)
            val token = sessionManager?.getToken() ?: handshakeRepository.currentSession.value?.token
            val baseServer = AppConfig.SERVER_URL.trimEnd('/')
            val streamUrl = StringBuilder("$baseServer/Videos/$episodeId/stream?static=true")
            if (!token.isNullOrBlank()) {
                streamUrl.append("&api_key=$token")
            }
            manager.startPreload(episodeId, Uri.parse(streamUrl.toString()))
        }
    }

    override fun onCleared() {
        super.onCleared()
        preloadJob?.cancel()
        preloadedEpisodeId?.let { episodeId ->
            preloadManager?.cancelPreload(episodeId)
        }
    }
}
