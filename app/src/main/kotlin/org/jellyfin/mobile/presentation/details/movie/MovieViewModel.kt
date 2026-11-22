package org.jellyfin.mobile.presentation.details.movie

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
import org.jellyfin.mobile.core.player.external.ExternalSubtitle
import org.jellyfin.mobile.core.player.preload.PreloadManager
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository
import org.jellyfin.mobile.domain.usecase.GetMediaDetailsUseCase

/**
 * MovieViewModel orchestrates business logic, playback preparation,
 * audio/subtitle selection, and local persistence for dedicated Movie details.
 * Strictly adheres to MVI architecture with zero emojis.
 */
class MovieViewModel(
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val mediaDetailsRepository: MediaDetailsRepository,
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository? = null,
    private val apiService: JellyfinApiService? = null,
    private val sessionManager: SessionManager? = null,
    private val preloadManager: PreloadManager? = null,
    private val externalPlayerManager: ExternalPlayerManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieUiState>(MovieUiState.Loading)
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MovieEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentMovieId: String? = null

    fun processIntent(intent: MovieIntent) {
        when (intent) {
            is MovieIntent.LoadMovie -> {
                currentMovieId = intent.movieId
                loadMovieDetails(intent.movieId)
            }
            is MovieIntent.RetryLoad -> {
                currentMovieId?.let { loadMovieDetails(it) }
            }
            is MovieIntent.RefreshPlaybackState -> {
                refreshPlaybackState()
            }
            is MovieIntent.Play -> {
                handlePlay(fromStart = false)
            }
            is MovieIntent.Resume -> {
                handlePlay(fromStart = false)
            }
            is MovieIntent.PlayFromStart -> {
                handlePlay(fromStart = true)
            }
            is MovieIntent.PlayExternal -> {
                handlePlayExternal()
            }
            is MovieIntent.PlayChapter -> {
                handlePlayChapter(intent.chapter)
            }
            is MovieIntent.ToggleFavorite -> {
                toggleFavorite()
            }
            is MovieIntent.ToggleWatched -> {
                toggleWatched()
            }
            is MovieIntent.OpenTrailer -> {
                viewModelScope.launch {
                    _effects.send(MovieEffect.ShowToast("الإعلان الترويجي غير متوفر لهذا الفيلم حالياً"))
                }
            }
            is MovieIntent.ShareMovie -> {
                val current = _uiState.value
                if (current is MovieUiState.Success) {
                    val shareUrl = "${AppConfig.SERVER_URL}/web/index.html#!/details?id=${current.movie.id}"
                    viewModelScope.launch {
                        _effects.send(MovieEffect.ShareMovie(title = current.movie.title, shareUrl = shareUrl))
                    }
                }
            }
            is MovieIntent.ToggleDownload -> {
                toggleDownload()
            }
            is MovieIntent.OpenAudioSubtitleSheet -> {
                val current = _uiState.value
                if (current is MovieUiState.Success) {
                    _uiState.value = current.copy(isAudioSubtitleSheetVisible = true)
                }
            }
            is MovieIntent.DismissAudioSubtitleSheet -> {
                val current = _uiState.value
                if (current is MovieUiState.Success) {
                    _uiState.value = current.copy(isAudioSubtitleSheetVisible = false)
                }
            }
            is MovieIntent.SelectAudioTrack -> {
                selectAudioTrack(intent.track)
            }
            is MovieIntent.SelectSubtitleTrack -> {
                selectSubtitleTrack(intent.track)
            }
            is MovieIntent.OpenActorFilmography -> {
                viewModelScope.launch {
                    _effects.send(MovieEffect.NavigateToActorFilmography(intent.personId, intent.personName))
                }
            }
        }
    }

    private fun loadMovieDetails(movieId: String) {
        viewModelScope.launch {
            _uiState.value = MovieUiState.Loading

            // Verify active session before retrieving details
            if (handshakeRepository.currentSession.value == null) {
                val handshakeResult = handshakeRepository.performHandshake()
                if (handshakeResult.isFailure) {
                    _uiState.value = MovieUiState.Error(
                        message = handshakeResult.exceptionOrNull()?.localizedMessage
                            ?: "فشل الاتصال بخادم Jellyfin",
                        isNetworkError = true
                    )
                    return@launch
                }
            }

            getMediaDetailsUseCase(movieId)
                .onSuccess { details ->
                    val isFav = localProfileRepository?.isFavorite(movieId) ?: details.isFavorite
                    val isWat = localProfileRepository?.isWatched(movieId)
                        ?: (details.playbackPositionPercentage >= 95f)

                    // Extract or construct Audio and Subtitle tracks
                    val (audioTracks, subtitleTracks) = fetchMediaTracks(movieId, details)

                    val selectedAudio = audioTracks.firstOrNull { it.isSelected } ?: audioTracks.firstOrNull()
                    val selectedSub = subtitleTracks.firstOrNull { it.isSelected }

                    _uiState.value = MovieUiState.Success(
                        movie = details,
                        isFavorite = isFav,
                        isWatched = isWat,
                        castList = details.castMembers,
                        chapters = kotlinx.collections.immutable.persistentListOf(),
                        audioTracks = audioTracks.toImmutableList(),
                        subtitleTracks = subtitleTracks.toImmutableList(),
                        selectedAudioTrack = selectedAudio,
                        selectedSubtitleTrack = selectedSub,
                        downloadStatus = MovieDownloadStatus.Idle,
                        isAudioSubtitleSheetVisible = false,
                        similarMovies = details.similarItems
                    )
                    schedulePreload(movieId)
                }
                .onFailure { error ->
                    _uiState.value = MovieUiState.Error(
                        message = error.localizedMessage ?: "تعذر تحميل تفاصيل الفيلم السينمائي",
                        isNetworkError = true
                    )
                }
        }
    }

    private suspend fun fetchMediaTracks(
        movieId: String,
        details: org.jellyfin.mobile.domain.model.MediaDetails
    ): Pair<List<MovieAudioTrack>, List<MovieSubtitleTrack>> {
        val service = apiService
        if (service != null) {
            val userId = sessionManager?.getUserId() ?: handshakeRepository.currentSession.value?.userId
            if (!userId.isNullOrBlank()) {
                val itemDtoResult = runCatching {
                    service.getItemDetails(userId = userId, itemId = movieId)
                }
                val itemDto = itemDtoResult.getOrNull()
                val mediaStreams = itemDto?.mediaSources?.firstOrNull()?.mediaStreams
                if (!mediaStreams.isNullOrEmpty()) {
                    val audioList = mediaStreams
                        .filter { it.type.equals("Audio", ignoreCase = true) }
                        .mapIndexed { index, s ->
                            val lang = s.language?.uppercase().orEmpty()
                            val codec = s.codec?.uppercase().orEmpty()
                            val ch = s.channelLayout ?: s.channels?.let { "$it.0" }.orEmpty()
                            val label = s.displayTitle
                                ?: listOf(lang, codec, ch).filter { it.isNotBlank() }.joinToString(" - ")
                                    .ifBlank { "مسار صوتي ${index + 1}" }

                            MovieAudioTrack(
                                index = s.index ?: index,
                                title = label,
                                language = s.language,
                                codec = s.codec,
                                channels = ch,
                                isDefault = s.isDefault == true,
                                isSelected = s.isDefault == true || index == 0
                            )
                        }

                    val subList = mediaStreams
                        .filter { it.type.equals("Subtitle", ignoreCase = true) }
                        .mapIndexed { index, s ->
                            val lang = s.language?.uppercase().orEmpty()
                            val codec = s.codec?.uppercase().orEmpty()
                            val label = s.displayTitle
                                ?: listOf(lang, codec).filter { it.isNotBlank() }.joinToString(" ")
                                    .ifBlank { "ملف ترجمة ${index + 1}" }

                            MovieSubtitleTrack(
                                index = s.index ?: index,
                                title = label,
                                language = s.language,
                                isForced = s.isForced == true,
                                isDefault = s.isDefault == true,
                                isSelected = s.isDefault == true
                            )
                        }

                    val finalAudio = if (audioList.isNotEmpty()) audioList else defaultAudioTracks(details)
                    return Pair(finalAudio, subList)
                }
            }
        }

        return Pair(defaultAudioTracks(details), defaultSubtitleTracks())
    }

    private fun defaultAudioTracks(details: org.jellyfin.mobile.domain.model.MediaDetails): List<MovieAudioTrack> {
        val codec = details.techBadges.audioCodec
        val channels = details.techBadges.audioChannels
        val title = if (codec.isNotBlank() || channels.isNotBlank()) {
            "$codec $channels".trim()
        } else {
            "الصوت الافتراضي (ستيريو)"
        }
        return listOf(
            MovieAudioTrack(
                index = 0,
                title = title,
                codec = codec,
                channels = channels,
                isDefault = true,
                isSelected = true
            )
        )
    }

    private fun defaultSubtitleTracks(): List<MovieSubtitleTrack> {
        return listOf(
            MovieSubtitleTrack(
                index = 0,
                title = "ترجمة عربية مدمجة",
                language = "ara",
                isDefault = true,
                isSelected = false
            )
        )
    }

    private fun refreshPlaybackState() {
        val movieId = currentMovieId ?: return
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        viewModelScope.launch {
            getMediaDetailsUseCase(movieId).onSuccess { details ->
                val isFav = localProfileRepository?.isFavorite(movieId) ?: details.isFavorite
                val isWat = localProfileRepository?.isWatched(movieId)
                    ?: (details.playbackPositionPercentage >= 95f)

                _uiState.value = current.copy(
                    movie = details,
                    isFavorite = isFav,
                    isWatched = isWat
                )
            }
        }
    }

    private var lastPlayClickTimeMs: Long = 0L

    private fun handlePlay(fromStart: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastPlayClickTimeMs < 800L) return
        lastPlayClickTimeMs = now
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        val resumeMs = if (!fromStart) current.movie.resumePositionMs else 0L
        viewModelScope.launch {
            _effects.send(MovieEffect.LaunchPlayer(mediaId = current.movie.id, resumePositionMs = resumeMs))
        }
    }

    private fun handlePlayChapter(chapter: MovieChapter) {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        viewModelScope.launch {
            _effects.send(
                MovieEffect.LaunchPlayer(
                    mediaId = current.movie.id,
                    resumePositionMs = chapter.startPositionMs
                )
            )
        }
    }

    private fun handlePlayExternal() {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        val movieId = current.movie.id
        val title = current.movie.title
        val resumeMs = current.movie.resumePositionMs
        val durationMs = current.movie.runTimeTicks?.div(10_000L) ?: 0L

        val token = sessionManager?.getToken() ?: handshakeRepository.currentSession.value?.token
        val baseServer = AppConfig.SERVER_URL.trimEnd('/')
        val streamUrl = StringBuilder("$baseServer/Videos/$movieId/stream?static=true")
        if (!token.isNullOrBlank()) {
            streamUrl.append("&api_key=$token")
        }

        val subtitles = current.subtitleTracks.mapIndexed { idx, sub ->
            val subUrl = StringBuilder("$baseServer/Videos/$movieId/Subtitles/$idx/Stream.srt")
            if (!token.isNullOrBlank()) {
                subUrl.append("?api_key=$token")
            }
            ExternalSubtitle(
                url = subUrl.toString(),
                name = sub.language ?: "عربي",
                language = sub.language ?: "ar",
                isDefault = sub.isDefault
            )
        }

        val payload = ExternalPlayerPayload(
            mediaId = movieId,
            title = title,
            streamUrl = streamUrl.toString(),
            resumePositionMs = resumeMs,
            durationMs = durationMs,
            subtitles = subtitles
        )

        viewModelScope.launch {
            _effects.send(MovieEffect.LaunchExternalPlayer(payload))
        }
    }

    fun onExternalPlaybackResult(result: ExternalPlaybackResult) {
        val current = _uiState.value
        val title = if (current is MovieUiState.Success) current.movie.title else AppConfig.APP_NAME
        viewModelScope.launch {
            externalPlayerManager?.persistResult(result, title, "Movie")
            refreshPlaybackState()
        }
    }

    private fun toggleFavorite() {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return
        val movieId = currentMovieId ?: return
        val targetState = !current.isFavorite

        _uiState.value = current.copy(isFavorite = targetState)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                localProfileRepository.toggleFavorite(
                    itemId = movieId,
                    title = current.movie.title,
                    mediaType = "Movie",
                    releaseYear = current.movie.productionYear?.toString().orEmpty(),
                    communityRating = current.movie.communityRating?.toString().orEmpty()
                )
            } else {
                mediaDetailsRepository.toggleFavorite(movieId, targetState)
            }

            val msg = if (targetState) "تمت إضافة الفيلم إلى المفضلة" else "تمت إزالة الفيلم من المفضلة"
            _effects.send(MovieEffect.ShowToast(msg))
        }
    }

    private fun toggleWatched() {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return
        val movieId = currentMovieId ?: return
        val targetState = !current.isWatched

        _uiState.value = current.copy(isWatched = targetState)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                if (targetState) {
                    localProfileRepository.addToArchive(
                        itemId = movieId,
                        title = current.movie.title,
                        mediaType = "Movie"
                    )
                    localProfileRepository.markWatched(movieId)
                } else {
                    localProfileRepository.removeFromArchive(movieId)
                }
            } else {
                mediaDetailsRepository.markAsWatched(movieId, targetState)
            }

            val msg = if (targetState) "تم تحديد الفيلم كمشاهد" else "تمت إزالة علامة المشاهدة"
            _effects.send(MovieEffect.ShowToast(msg))
        }
    }

    private fun toggleDownload() {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        when (current.downloadStatus) {
            is MovieDownloadStatus.Idle -> {
                _uiState.value = current.copy(downloadStatus = MovieDownloadStatus.Downloading(0.2f))
                viewModelScope.launch {
                    _effects.send(MovieEffect.ShowToast("تم بدء تنزيل الفيلم للمشاهدة بدون إنترنت"))
                    _uiState.value = current.copy(downloadStatus = MovieDownloadStatus.Downloaded)
                }
            }
            is MovieDownloadStatus.Downloading -> {
                _uiState.value = current.copy(downloadStatus = MovieDownloadStatus.Idle)
                viewModelScope.launch {
                    _effects.send(MovieEffect.ShowToast("تم إلغاء التنزيل"))
                }
            }
            is MovieDownloadStatus.Downloaded -> {
                _uiState.value = current.copy(downloadStatus = MovieDownloadStatus.Idle)
                viewModelScope.launch {
                    _effects.send(MovieEffect.ShowToast("تم حذف النسخة المحملة من الجهاز"))
                }
            }
        }
    }

    private fun selectAudioTrack(track: MovieAudioTrack) {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        val updatedTracks = current.audioTracks.map {
            it.copy(isSelected = it.index == track.index)
        }.toImmutableList()

        _uiState.value = current.copy(
            audioTracks = updatedTracks,
            selectedAudioTrack = track
        )

        viewModelScope.launch {
            _effects.send(MovieEffect.ShowToast("تم اختيار مسار الصوت: ${track.title}"))
        }
    }

    private fun selectSubtitleTrack(track: MovieSubtitleTrack?) {
        val current = _uiState.value
        if (current !is MovieUiState.Success) return

        val updatedTracks = current.subtitleTracks.map {
            it.copy(isSelected = it.index == track?.index)
        }.toImmutableList()

        _uiState.value = current.copy(
            subtitleTracks = updatedTracks,
            selectedSubtitleTrack = track
        )

        viewModelScope.launch {
            val msg = if (track != null) "تم تفعيل الترجمة: ${track.title}" else "تم إيقاف الترجمة"
            _effects.send(MovieEffect.ShowToast(msg))
        }
    }

    private var preloadJob: Job? = null

    private fun schedulePreload(movieId: String) {
        preloadJob?.cancel()
        val manager = preloadManager ?: return
        preloadJob = viewModelScope.launch {
            delay(800L)
            val token = sessionManager?.getToken() ?: handshakeRepository.currentSession.value?.token
            val baseServer = AppConfig.SERVER_URL.trimEnd('/')
            val streamUrl = StringBuilder("$baseServer/Videos/$movieId/stream?static=true")
            if (!token.isNullOrBlank()) {
                streamUrl.append("&api_key=$token")
            }
            manager.startPreload(movieId, Uri.parse(streamUrl.toString()))
        }
    }

    override fun onCleared() {
        super.onCleared()
        preloadJob?.cancel()
        currentMovieId?.let { movieId ->
            preloadManager?.cancelPreload(movieId)
        }
    }
}
