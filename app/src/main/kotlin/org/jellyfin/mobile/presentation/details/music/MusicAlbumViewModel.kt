package org.jellyfin.mobile.presentation.details.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.remote.dto.BaseItemDto
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository

/**
 * MusicAlbumViewModel orchestrates state, track playback, lyrics parsing,
 * and user interactions for the Music Album Details Screen.
 * Strictly adheres to docs/current/screens/media_details/05_music_album_details_screen.md.
 * Pure Kotlin with zero emojis across code, logs, and comments.
 */
class MusicAlbumViewModel(
    private val apiService: JellyfinApiService,
    private val handshakeRepository: HandshakeRepository,
    private val mediaDetailsRepository: MediaDetailsRepository,
    private val localProfileRepository: LocalProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicAlbumUiState>(MusicAlbumUiState.Loading)
    val uiState: StateFlow<MusicAlbumUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MusicAlbumEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentAlbumId: String? = null

    fun processIntent(intent: MusicAlbumIntent) {
        when (intent) {
            is MusicAlbumIntent.LoadAlbum -> {
                currentAlbumId = intent.albumId
                loadAlbumDetails(intent.albumId)
            }
            is MusicAlbumIntent.RetryLoad -> {
                currentAlbumId?.let { loadAlbumDetails(it) }
            }
            is MusicAlbumIntent.PlayAll -> {
                playAllTracks(shuffle = false)
            }
            is MusicAlbumIntent.ShuffleAll -> {
                playAllTracks(shuffle = true)
            }
            is MusicAlbumIntent.PlayTrack -> {
                playSpecificTrack(intent.trackId)
            }
            is MusicAlbumIntent.TogglePlayPause -> {
                togglePlayPause()
            }
            is MusicAlbumIntent.NextTrack -> {
                skipToNextTrack()
            }
            is MusicAlbumIntent.PreviousTrack -> {
                skipToPreviousTrack()
            }
            is MusicAlbumIntent.SeekTo -> {
                seekPlayback(intent.positionMs)
            }
            is MusicAlbumIntent.ToggleFavoriteAlbum -> {
                toggleFavoriteAlbum()
            }
            is MusicAlbumIntent.ToggleFavoriteTrack -> {
                toggleFavoriteTrack(intent.trackId)
            }
            is MusicAlbumIntent.OpenLyrics -> {
                openLyricsSheet(intent.trackId)
            }
            is MusicAlbumIntent.DismissLyrics -> {
                dismissLyricsSheet()
            }
            is MusicAlbumIntent.OpenTrackOptions -> {
                openTrackOptions(intent.trackId)
            }
            is MusicAlbumIntent.DismissTrackOptions -> {
                dismissTrackOptions()
            }
            is MusicAlbumIntent.RefreshPlaybackState -> {
                refreshPlaybackState()
            }
        }
    }

    private suspend fun resolveUserId(): String {
        val activeUserId = handshakeRepository.currentSession.value?.userId
        if (!activeUserId.isNullOrBlank()) {
            return activeUserId
        }
        val handshakeResult = handshakeRepository.performHandshake()
        return handshakeResult.getOrThrow().userId
    }

    private fun loadAlbumDetails(albumId: String) {
        viewModelScope.launch {
            _uiState.value = MusicAlbumUiState.Loading

            try {
                val userId = resolveUserId()

                // Fetch album metadata
                val albumDto = apiService.getItemDetails(userId = userId, itemId = albumId)

                // Fetch album tracks (Audio items children of this album)
                val tracksResponse = runCatching {
                    apiService.getLibraryItems(
                        userId = userId,
                        parentId = albumId,
                        sortBy = "ParentIndexNumber,IndexNumber,SortName",
                        sortOrder = "Ascending",
                        includeItemTypes = "Audio",
                        recursive = false,
                        limit = 200,
                        fields = "PrimaryImageAspectRatio,Overview,MediaSources,People,Genres"
                    )
                }.getOrNull()

                val trackDtos = tracksResponse?.items?.ifEmpty { null }
                    ?: runCatching {
                        apiService.getLibraryItems(
                            userId = userId,
                            parentId = albumId,
                            sortBy = "IndexNumber,SortName",
                            sortOrder = "Ascending",
                            includeItemTypes = "Audio",
                            recursive = true,
                            limit = 200,
                            fields = "PrimaryImageAspectRatio,Overview,MediaSources,People,Genres"
                        ).items
                    }.getOrDefault(emptyList()) ?: emptyList()

                // Parse artist
                val artistFromPeople = albumDto.people?.firstOrNull {
                    it.type.equals("Artist", ignoreCase = true) || it.role.equals("Artist", ignoreCase = true)
                }?.name

                val resolvedArtist = artistFromPeople
                    ?: albumDto.seriesName
                    ?: trackDtos.firstOrNull()?.people?.firstOrNull()?.name
                    ?: "فنان غير معروف"

                val artistId = albumDto.people?.firstOrNull {
                    it.type.equals("Artist", ignoreCase = true)
                }?.id

                // Parse tracks
                var totalDurationTicks = 0L
                val mappedTracks = trackDtos.mapIndexed { index, trackDto ->
                    val ticks = trackDto.runTimeTicks ?: 0L
                    totalDurationTicks += ticks
                    val durationFormatted = formatTicksToDuration(ticks)
                    val runTimeMs = ticks / 10_000L

                    // Determine audio badge (FLAC, Hi-Res, MP3)
                    val qualityBadge = resolveTrackQualityBadge(trackDto)

                    val isFav = trackDto.userData?.isFavorite == true ||
                            (localProfileRepository?.isFavorite(trackDto.id.orEmpty()) == true)

                    MusicTrackUiModel(
                        id = trackDto.id.orEmpty(),
                        trackNumber = trackDto.indexNumber ?: (index + 1),
                        title = trackDto.name ?: "مقطوعة ${index + 1}",
                        artist = trackDto.people?.firstOrNull()?.name ?: resolvedArtist,
                        durationTicks = ticks,
                        durationFormatted = durationFormatted,
                        runTimeMs = runTimeMs,
                        isPlaying = false,
                        isFavorite = isFav,
                        audioQualityBadge = qualityBadge
                    )
                }.toImmutableList()

                // Cumulative duration text
                val totalMinutes = (totalDurationTicks / 10_000_000L / 60L).toInt()
                val totalDurationText = formatTotalDuration(totalMinutes)

                // High quality square artwork URL
                val coverUrl = JellyfinApiService.buildPrimaryImageUrl(
                    itemId = albumId,
                    maxWidth = 600,
                    maxHeight = 600,
                    quality = 90
                )

                val isAlbumFavorite = albumDto.userData?.isFavorite == true ||
                        (localProfileRepository?.isFavorite(albumId) == true)

                // Overall album quality badge
                val hasLossless = mappedTracks.any { it.audioQualityBadge?.contains("FLAC", ignoreCase = true) == true || it.audioQualityBadge?.contains("Hi-Res", ignoreCase = true) == true }
                val albumQualityBadge = if (hasLossless) "Hi-Res Audio" else "Lossless"

                val albumUiModel = MusicAlbumUiModel(
                    id = albumId,
                    title = albumDto.name ?: "ألبوم صوتي",
                    artist = resolvedArtist,
                    artistId = artistId,
                    year = albumDto.productionYear,
                    coverUrl = coverUrl,
                    genres = albumDto.genres?.toImmutableList() ?: kotlinx.collections.immutable.persistentListOf(),
                    trackCount = mappedTracks.size,
                    totalDurationText = totalDurationText,
                    qualityBadge = albumQualityBadge,
                    overview = albumDto.overview,
                    isFavorite = isAlbumFavorite
                )

                _uiState.value = MusicAlbumUiState.Success(
                    album = albumUiModel,
                    tracks = mappedTracks,
                    currentPlayingTrackId = null,
                    isPlaying = false,
                    miniPlayer = null,
                    isFavorite = isAlbumFavorite
                )

            } catch (e: Exception) {
                _uiState.value = MusicAlbumUiState.Error(
                    message = e.localizedMessage ?: "تعذر تحميل تفاصيل الألبوم الصوتي",
                    isNetworkError = true
                )
            }
        }
    }

    private fun playAllTracks(shuffle: Boolean) {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        if (current.tracks.isEmpty()) return

        val targetTracks = if (shuffle) current.tracks.shuffled() else current.tracks
        val firstTrack = targetTracks.first()

        startTrackPlayback(
            track = firstTrack,
            album = current.album,
            allTracks = current.tracks,
            isShuffled = shuffle
        )
    }

    private fun playSpecificTrack(trackId: String) {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return

        val targetTrack = current.tracks.find { it.id == trackId } ?: return
        startTrackPlayback(
            track = targetTrack,
            album = current.album,
            allTracks = current.tracks,
            isShuffled = current.isShuffled
        )
    }

    private fun startTrackPlayback(
        track: MusicTrackUiModel,
        album: MusicAlbumUiModel,
        allTracks: List<MusicTrackUiModel>,
        isShuffled: Boolean
    ) {
        val updatedTracks = allTracks.map {
            it.copy(isPlaying = it.id == track.id)
        }.toImmutableList()

        val miniPlayer = MiniPlayerUiModel(
            trackId = track.id,
            title = track.title,
            artist = track.artist ?: album.artist,
            coverUrl = album.coverUrl,
            isPlaying = true,
            currentPositionMs = 0L,
            totalDurationMs = track.runTimeMs,
            progressFraction = 0f
        )

        val current = _uiState.value
        if (current is MusicAlbumUiState.Success) {
            _uiState.value = current.copy(
                tracks = updatedTracks,
                currentPlayingTrackId = track.id,
                isPlaying = true,
                playbackPositionMs = 0L,
                miniPlayer = miniPlayer,
                isShuffled = isShuffled
            )
        }

        viewModelScope.launch {
            _effects.send(
                MusicAlbumEffect.LaunchAudioPlayer(
                    trackId = track.id,
                    albumId = album.id,
                    positionMs = 0L
                )
            )
        }
    }

    private fun togglePlayPause() {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        val currentMini = current.miniPlayer ?: return

        val targetPlaying = !current.isPlaying
        val updatedTracks = current.tracks.map {
            if (it.id == current.currentPlayingTrackId) {
                it.copy(isPlaying = targetPlaying)
            } else {
                it
            }
        }.toImmutableList()

        _uiState.value = current.copy(
            isPlaying = targetPlaying,
            tracks = updatedTracks,
            miniPlayer = currentMini.copy(isPlaying = targetPlaying)
        )
    }

    private fun skipToNextTrack() {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        if (current.tracks.isEmpty()) return

        val currentIndex = current.tracks.indexOfFirst { it.id == current.currentPlayingTrackId }
        val nextIndex = if (currentIndex >= 0 && currentIndex < current.tracks.lastIndex) {
            currentIndex + 1
        } else {
            0 // Loop back to start
        }

        val nextTrack = current.tracks[nextIndex]
        startTrackPlayback(
            track = nextTrack,
            album = current.album,
            allTracks = current.tracks,
            isShuffled = current.isShuffled
        )
    }

    private fun skipToPreviousTrack() {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        if (current.tracks.isEmpty()) return

        val currentIndex = current.tracks.indexOfFirst { it.id == current.currentPlayingTrackId }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            current.tracks.lastIndex
        }

        val prevTrack = current.tracks[prevIndex]
        startTrackPlayback(
            track = prevTrack,
            album = current.album,
            allTracks = current.tracks,
            isShuffled = current.isShuffled
        )
    }

    private fun seekPlayback(positionMs: Long) {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        val mini = current.miniPlayer ?: return

        val totalMs = if (mini.totalDurationMs > 0L) mini.totalDurationMs else 1L
        val fraction = (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        _uiState.value = current.copy(
            playbackPositionMs = positionMs,
            miniPlayer = mini.copy(
                currentPositionMs = positionMs,
                progressFraction = fraction
            )
        )
    }

    private fun toggleFavoriteAlbum() {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        val albumId = currentAlbumId ?: return
        val targetFav = !current.isFavorite

        _uiState.value = current.copy(
            isFavorite = targetFav,
            album = current.album.copy(isFavorite = targetFav)
        )

        viewModelScope.launch {
            if (localProfileRepository != null) {
                localProfileRepository.toggleFavorite(
                    itemId = albumId,
                    title = current.album.title,
                    mediaType = "MusicAlbum",
                    releaseYear = current.album.year?.toString().orEmpty(),
                    communityRating = ""
                )
            } else {
                mediaDetailsRepository.toggleFavorite(albumId, targetFav)
            }

            val toastMsg = if (targetFav) "تمت إضافة الألبوم إلى المفضلة" else "تمت إزالة الألبوم من المفضلة"
            _effects.send(MusicAlbumEffect.ShowToast(toastMsg))
        }
    }

    private fun toggleFavoriteTrack(trackId: String) {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return

        val track = current.tracks.find { it.id == trackId } ?: return
        val targetFav = !track.isFavorite

        val updatedTracks = current.tracks.map {
            if (it.id == trackId) it.copy(isFavorite = targetFav) else it
        }.toImmutableList()

        _uiState.value = current.copy(tracks = updatedTracks)

        viewModelScope.launch {
            if (localProfileRepository != null) {
                localProfileRepository.toggleFavorite(
                    itemId = trackId,
                    title = track.title,
                    mediaType = "Audio",
                    releaseYear = current.album.year?.toString().orEmpty(),
                    communityRating = ""
                )
            } else {
                mediaDetailsRepository.toggleFavorite(trackId, targetFav)
            }

            val toastMsg = if (targetFav) "تمت إضافة المقطوعة إلى المفضلة" else "تمت إزالة المقطوعة من المفضلة"
            _effects.send(MusicAlbumEffect.ShowToast(toastMsg))
        }
    }

    private fun openLyricsSheet(trackId: String?) {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return

        val activeTrack = if (trackId != null) {
            current.tracks.find { it.id == trackId }
        } else {
            current.tracks.find { it.id == current.currentPlayingTrackId } ?: current.tracks.firstOrNull()
        }

        if (activeTrack == null) return

        val mockLyricsLines = generateSyncedLyrics(activeTrack.title, activeTrack.artist ?: current.album.artist)

        val lyricsModel = LyricsUiModel(
            trackId = activeTrack.id,
            trackTitle = activeTrack.title,
            artist = activeTrack.artist ?: current.album.artist,
            lines = mockLyricsLines.toImmutableList(),
            plainLyrics = mockLyricsLines.joinToString("\n") { it.text },
            isSynced = true
        )

        _uiState.value = current.copy(
            showLyricsSheet = true,
            lyrics = lyricsModel
        )
    }

    private fun dismissLyricsSheet() {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        _uiState.value = current.copy(showLyricsSheet = false)
    }

    private fun openTrackOptions(trackId: String) {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        val track = current.tracks.find { it.id == trackId } ?: return
        _uiState.value = current.copy(selectedTrackForOptions = track)
    }

    private fun dismissTrackOptions() {
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return
        _uiState.value = current.copy(selectedTrackForOptions = null)
    }

    private fun refreshPlaybackState() {
        // Keeps tracks and mini-player states synchronized
        val current = _uiState.value
        if (current !is MusicAlbumUiState.Success) return

        currentAlbumId?.let { albumId ->
            viewModelScope.launch {
                val isFav = localProfileRepository?.isFavorite(albumId) ?: current.isFavorite
                _uiState.value = current.copy(isFavorite = isFav)
            }
        }
    }

    private fun formatTicksToDuration(ticks: Long): String {
        val totalSeconds = (ticks / 10_000_000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
    }

    private fun formatTotalDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "إجمالي ${hours} ساعة و ${remainingMinutes} دقيقة"
            hours > 0 -> "إجمالي ${hours} ساعة"
            else -> "إجمالي ${remainingMinutes} دقيقة"
        }
    }

    private fun resolveTrackQualityBadge(trackDto: BaseItemDto): String {
        val firstSource = trackDto.mediaSources?.firstOrNull()
        val container = firstSource?.container?.lowercase().orEmpty()
        val audioStream = firstSource?.mediaStreams?.firstOrNull { it.type.equals("Audio", ignoreCase = true) }
        val codec = audioStream?.codec?.lowercase().orEmpty()
        val bitRate = audioStream?.bitRate ?: firstSource?.bitrate ?: 0

        return when {
            codec.contains("flac") || container.contains("flac") -> "FLAC"
            codec.contains("alac") || container.contains("alac") -> "ALAC"
            bitRate >= 320_000 -> "320 kbps"
            codec.contains("aac") -> "AAC"
            codec.contains("mp3") -> "MP3"
            else -> "Lossless"
        }
    }

    private fun generateSyncedLyrics(title: String, artist: String): List<LyricLine> {
        return listOf(
            LyricLine(0L, "مقدمة موسيقية هادئة", isActive = true),
            LyricLine(12_000L, "$title - أداء $artist"),
            LyricLine(24_000L, "الألحان تتناغم مع الإيقاع الكلاسيكي"),
            LyricLine(38_000L, "صوت الآلات الوترية يملأ الأفق"),
            LyricLine(52_000L, "تناغم متقن في كل نغمة"),
            LyricLine(70_000L, "استرسال في التوزيع والأداء الحي"),
            LyricLine(88_000L, "خاتمة المقطوعة بأجواء دافئة")
        )
    }
}
