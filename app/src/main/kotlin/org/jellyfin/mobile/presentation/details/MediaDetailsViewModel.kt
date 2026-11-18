package org.jellyfin.mobile.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.model.SeasonInfo
import org.jellyfin.mobile.domain.model.toEpisodeDomainModel
import org.jellyfin.mobile.domain.model.toMediaDetailDomainModel
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository
import org.jellyfin.mobile.domain.usecase.GetMediaDetailsUseCase

/**
 * MediaDetailsViewModel manages state transitions, pre-playback configurations,
 * and user interactions for the Media Details Screen adhering to MVI architecture.
 */
class MediaDetailsViewModel(
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val mediaDetailsRepository: MediaDetailsRepository,
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaDetailsUiState>(MediaDetailsUiState.Loading)
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MediaDetailsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentMediaId: String? = null

    fun processIntent(intent: MediaDetailsIntent) {
        when (intent) {
            is MediaDetailsIntent.LoadMedia -> {
                currentMediaId = intent.mediaId
                loadMedia(intent.mediaId)
            }
            is MediaDetailsIntent.RetryLoad -> {
                currentMediaId?.let { loadMedia(it) }
            }
            is MediaDetailsIntent.SelectSeason -> {
                selectSeasonByNumber(intent.seasonNumber)
            }
            is MediaDetailsIntent.SelectSeasonById -> {
                selectSeasonById(intent.seasonId)
            }
            is MediaDetailsIntent.ToggleFavorite -> {
                toggleFavorite()
            }
            is MediaDetailsIntent.ToggleWatched -> {
                toggleWatched()
            }
            is MediaDetailsIntent.PlayMedia -> {
                handlePlayMedia(fromStart = false)
            }
            is MediaDetailsIntent.ResumeMedia -> {
                handlePlayMedia(fromStart = false)
            }
            is MediaDetailsIntent.PlayFromStart -> {
                handlePlayMedia(fromStart = true)
            }
            is MediaDetailsIntent.PlayEpisode -> {
                handlePlayEpisode(intent.episodeId)
            }
            is MediaDetailsIntent.ToggleEpisodeWatched -> {
                toggleEpisodeWatched(intent.episodeId)
            }
            is MediaDetailsIntent.DownloadEpisode -> {
                viewModelScope.launch {
                    _effects.send(MediaDetailsEffect.ShowToast("بدء التنزيل قيد التطوير"))
                }
            }
            is MediaDetailsIntent.OpenActorFilmography -> {
                viewModelScope.launch {
                    _effects.send(MediaDetailsEffect.NavigateToActorFilmography(intent.personId, intent.personName))
                }
            }
            is MediaDetailsIntent.OpenTechInspector -> {
                updateInspectorState(isOpen = true)
            }
            is MediaDetailsIntent.DismissTechInspector -> {
                updateInspectorState(isOpen = false)
            }
            is MediaDetailsIntent.OpenTrailer -> {
                viewModelScope.launch {
                    _effects.send(MediaDetailsEffect.ShowToast("الإعلانات الترويجية غير متوفرة لهذا العمل"))
                }
            }
        }
    }

    private fun loadMedia(mediaId: String) {
        viewModelScope.launch {
            _uiState.value = MediaDetailsUiState.Loading

            // Verify active session
            if (handshakeRepository.currentSession.value == null) {
                val handshakeResult = handshakeRepository.performHandshake()
                if (handshakeResult.isFailure) {
                    _uiState.value = MediaDetailsUiState.Error(
                        message = handshakeResult.exceptionOrNull()?.localizedMessage
                            ?: "فشل الاتصال بخادم Jellyfin",
                        isNetworkError = true
                    )
                    return@launch
                }
            }

            getMediaDetailsUseCase(mediaId)
                .onSuccess { details ->
                    val defaultSeason = details.seasons.firstOrNull()
                    val defaultSeasonNum = defaultSeason?.indexNumber ?: 1

                    val episodesForSeasonList = if (defaultSeason != null) {
                        details.episodes.filter { it.seasonId == defaultSeason.id }.ifEmpty { details.episodes }
                    } else {
                        details.episodes
                    }

                    val episodesForSeasonModels = episodesForSeasonList.map {
                        it.toEpisodeDomainModel(defaultSeasonNum)
                    }.toImmutableList()

                    val seasonInfos = details.seasons.map {
                        SeasonInfo(
                            seasonNumber = it.indexNumber ?: 1,
                            seasonName = it.name,
                            episodeCount = it.episodeCount
                        )
                    }.toImmutableList()

                    val nextUp = episodesForSeasonModels.firstOrNull { !it.isWatched }
                        ?: episodesForSeasonModels.firstOrNull()

                    val isFav = localProfileRepository?.isFavorite(mediaId) ?: details.isFavorite
                    val isWat = localProfileRepository?.isWatched(mediaId) ?: (details.playbackPositionPercentage >= 95f)

                    _uiState.value = MediaDetailsUiState.Success(
                        mediaDetails = details,
                        mediaItem = details.toMediaDetailDomainModel(),
                        nextUpEpisode = nextUp,
                        seasons = seasonInfos,
                        selectedSeasonNumber = defaultSeasonNum,
                        selectedSeasonId = defaultSeason?.id,
                        episodesForSeason = episodesForSeasonModels,
                        episodesForSelectedSeason = episodesForSeasonList.toImmutableList(),
                        castList = details.castMembers,
                        isInspectorOpen = false,
                        isFavorite = isFav,
                        isWatched = isWat
                    )
                }
                .onFailure { error ->
                    _uiState.value = MediaDetailsUiState.Error(
                        message = error.localizedMessage ?: "تعذر تحميل تفاصيل العمل الفني",
                        isNetworkError = true
                    )
                }
        }
    }

    private fun selectSeasonByNumber(seasonNumber: Int) {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return

        val targetSeason = current.mediaDetails.seasons.find { it.indexNumber == seasonNumber }
        if (targetSeason != null) {
            selectSeasonById(targetSeason.id, seasonNumber)
        } else {
            _uiState.value = current.copy(selectedSeasonNumber = seasonNumber)
        }
    }

    private fun selectSeasonById(seasonId: String, seasonNumber: Int? = null) {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return

        val seriesId = currentMediaId ?: return
        val effectiveSeasonNum = seasonNumber
            ?: current.mediaDetails.seasons.find { it.id == seasonId }?.indexNumber
            ?: 1

        viewModelScope.launch {
            // First check if already loaded in full episode list
            val filtered = current.mediaDetails.episodes.filter { it.seasonId == seasonId }
            if (filtered.isNotEmpty()) {
                val models = filtered.map { it.toEpisodeDomainModel(effectiveSeasonNum) }.toImmutableList()
                _uiState.value = current.copy(
                    selectedSeasonId = seasonId,
                    selectedSeasonNumber = effectiveSeasonNum,
                    episodesForSeason = models,
                    episodesForSelectedSeason = filtered.toImmutableList(),
                    isLoadingEpisodes = false
                )
                return@launch
            }

            // Otherwise, fetch episodes for this specific season dynamically from repository
            _uiState.value = current.copy(
                selectedSeasonId = seasonId,
                selectedSeasonNumber = effectiveSeasonNum,
                isLoadingEpisodes = true
            )
            mediaDetailsRepository.getEpisodesForSeason(seriesId = seriesId, seasonId = seasonId)
                .onSuccess { seasonEpisodes ->
                    val models = seasonEpisodes.map { it.toEpisodeDomainModel(effectiveSeasonNum) }.toImmutableList()
                    _uiState.value = current.copy(
                        selectedSeasonId = seasonId,
                        selectedSeasonNumber = effectiveSeasonNum,
                        episodesForSeason = models,
                        episodesForSelectedSeason = seasonEpisodes.toImmutableList(),
                        isLoadingEpisodes = false
                    )
                }
                .onFailure {
                    _uiState.value = current.copy(isLoadingEpisodes = false)
                    _effects.send(MediaDetailsEffect.ShowToast("تعذر تحميل حلقات هذا الموسم"))
                }
        }
    }

    private fun toggleFavorite() {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return
        val mediaId = currentMediaId ?: return
        val targetState = !current.isFavorite

        // Optimistic UI update
        _uiState.value = current.copy(isFavorite = targetState)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                localProfileRepository.toggleFavorite(
                    itemId = mediaId,
                    title = current.mediaDetails.title,
                    mediaType = if (current.mediaDetails.isMovie) "Movie" else "Series",
                    releaseYear = current.mediaDetails.productionYear?.toString().orEmpty(),
                    communityRating = current.mediaDetails.communityRating?.toString().orEmpty()
                )
            } else {
                mediaDetailsRepository.toggleFavorite(mediaId, targetState)
            }
            val msg = if (targetState) "تمت الإضافة إلى المفضلة ❤️" else "تمت الإزالة من المفضلة"
            _effects.send(MediaDetailsEffect.ShowToast(msg))
        }
    }

    private fun toggleWatched() {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return
        val mediaId = currentMediaId ?: return
        val targetState = !current.isWatched

        _uiState.value = current.copy(isWatched = targetState)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                if (targetState) {
                    localProfileRepository.addToArchive(
                        itemId = mediaId,
                        title = current.mediaDetails.title,
                        mediaType = if (current.mediaDetails.isMovie) "Movie" else "Series"
                    )
                    localProfileRepository.markWatched(mediaId)
                } else {
                    localProfileRepository.removeFromArchive(mediaId)
                }
            } else {
                mediaDetailsRepository.markAsWatched(mediaId, targetState)
            }
            val msg = if (targetState) "تم التحديد كمشاهد ✓" else "تمت إزالة علامة المشاهدة"
            _effects.send(MediaDetailsEffect.ShowToast(msg))
        }
    }

    private fun toggleEpisodeWatched(episodeId: String) {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return

        val episode = current.episodesForSeason.find { it.id == episodeId } ?: return
        val targetWatched = !episode.isWatched

        val updatedModels = current.episodesForSeason.map {
            if (it.id == episodeId) it.copy(isPlayed = targetWatched) else it
        }.toImmutableList()

        _uiState.value = current.copy(episodesForSeason = updatedModels)

        viewModelScope.launch {
            mediaDetailsRepository.markAsWatched(episodeId, targetWatched)
                .onSuccess {
                    val msg = if (targetWatched) "تم تحديد الحلقة كمشاهدة ✓" else "تمت إزالة علامة المشاهدة"
                    _effects.send(MediaDetailsEffect.ShowToast(msg))
                }
                .onFailure {
                    // Revert on error
                    val reverted = current.episodesForSeason.map {
                        if (it.id == episodeId) it.copy(isPlayed = !targetWatched) else it
                    }.toImmutableList()
                    _uiState.value = current.copy(episodesForSeason = reverted)
                    _effects.send(MediaDetailsEffect.ShowToast("فشل تحديث حالة الحلقة"))
                }
        }
    }

    private fun handlePlayMedia(fromStart: Boolean) {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return
        val mediaId = currentMediaId ?: return

        // If it is a series, resolve the playable episode instead of seriesId
        val targetEpisode = if (!current.mediaDetails.isMovie) {
            current.mediaDetails.episodes.firstOrNull { !it.isWatched }
                ?: current.episodesForSelectedSeason.firstOrNull { !it.isWatched }
                ?: current.mediaDetails.episodes.firstOrNull()
                ?: current.episodesForSelectedSeason.firstOrNull()
        } else null

        val targetMediaId = targetEpisode?.id ?: mediaId

        val resumeTicks = if (!fromStart) {
            if (targetEpisode != null && targetEpisode.playbackPositionPercentage > 0f) {
                val totalTicks = targetEpisode.runTimeTicks ?: 0L
                (totalTicks * (targetEpisode.playbackPositionPercentage / 100.0)).toLong()
            } else if (current.mediaDetails.playbackPositionPercentage > 0f) {
                val totalTicks = current.mediaDetails.runTimeTicks ?: 0L
                (totalTicks * (current.mediaDetails.playbackPositionPercentage / 100.0)).toLong()
            } else {
                0L
            }
        } else {
            0L
        }

        val resumeMs = resumeTicks / 10_000L

        viewModelScope.launch {
            _effects.send(MediaDetailsEffect.LaunchPlayer(mediaId = targetMediaId, resumePositionMs = resumeMs))
        }
    }

    private fun handlePlayEpisode(episodeId: String) {
        val current = _uiState.value
        if (current !is MediaDetailsUiState.Success) return

        val episode = current.episodesForSelectedSeason.find { it.id == episodeId }
            ?: current.mediaDetails.episodes.find { it.id == episodeId }

        val resumeTicks = if (episode != null && episode.playbackPositionPercentage > 0f) {
            val totalTicks = episode.runTimeTicks ?: 0L
            (totalTicks * (episode.playbackPositionPercentage / 100.0)).toLong()
        } else {
            0L
        }

        val resumeMs = resumeTicks / 10_000L

        viewModelScope.launch {
            _effects.send(MediaDetailsEffect.LaunchPlayer(mediaId = episodeId, resumePositionMs = resumeMs))
        }
    }

    private fun updateInspectorState(isOpen: Boolean) {
        val current = _uiState.value
        if (current is MediaDetailsUiState.Success) {
            _uiState.value = current.copy(isInspectorOpen = isOpen)
        }
    }
}
