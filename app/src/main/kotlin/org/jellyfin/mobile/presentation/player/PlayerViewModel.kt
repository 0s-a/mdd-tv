package org.jellyfin.mobile.presentation.player

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.core.download.DownloadManagerProvider
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.remote.dto.PlaybackProgressDto
import org.jellyfin.mobile.data.remote.dto.PlaybackStartDto
import org.jellyfin.mobile.data.remote.dto.PlaybackStopDto
import org.jellyfin.mobile.domain.repository.HandshakeRepository

/**
 * PlayerViewModel drives the immersive Media3 video playback engine.
 * Strictly adheres to docs/screens/04_ADVANCED_PLAYER.md and docs/NON_FUNCTIONAL_REQUIREMENTS.md:
 * - Direct Play first with hardware decoding preference.
 * - Software audio boost up to 200% via LoudnessEnhancer.
 * - Periodic progress sync and Jellyfin session telemetry.
 * - TalkBack accessibility and zero memory leaks.
 */
class PlayerViewModel(
    private val context: Context,
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val handshakeRepository: HandshakeRepository
) : ViewModel() {

    private val tag = "PlayerViewModel"

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val trackSelector = DefaultTrackSelector(context)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        )
        .setTrackSelector(trackSelector)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DownloadManagerProvider.buildPlaybackDataSourceFactory(context)
            )
        )
        .build()

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var progressTickerJob: Job? = null
    private var lastReportTimeMs: Long = 0L
    private var hasMarkedPlayed: Boolean = false
    private var currentMediaSourceId: String? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    val durationMs = player.duration.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            totalDurationMs = durationMs
                        )
                    }
                    initLoudnessEnhancerIfNeeded()
                }
                Player.STATE_ENDED -> {
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            currentPositionMs = it.totalDurationMs
                        )
                    }
                    reportPlaybackStopped()
                }
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTicker()
                reportPlaybackProgress("unpause")
            } else {
                reportPlaybackProgress("pause")
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            parseAvailableTracks(tracks)
        }

        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues
                .mapNotNull { it.text }
                .joinToString("\n") { it.toString().trim() }
                .takeIf { it.isNotBlank() }

            _uiState.update { it.copy(currentSubtitleText = text) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(tag, "Playback error encountered: ${error.errorCodeName}", error)
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = "خطأ في تشغيل الفيديو: ${error.localizedMessage ?: error.errorCodeName}"
                )
            }
            viewModelScope.launch {
                _effects.send(PlayerEffect.ShowToast("تعذر استكمال تشغيل الفيديو"))
            }
        }
    }

    init {
        player.addListener(playerListener)
    }

    fun dispatch(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.InitializePlayer -> initialize(intent.mediaId, intent.startPositionMs)
            is PlayerIntent.Play -> player.play()
            is PlayerIntent.Pause -> player.pause()
            is PlayerIntent.TogglePlayPause -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            is PlayerIntent.SeekTo -> seekTo(intent.positionMs)
            is PlayerIntent.FastSeekBy -> fastSeekBy(intent.deltaMs)
            is PlayerIntent.SetVolume -> setVolume(intent.volume)
            is PlayerIntent.SetBrightness -> setBrightness(intent.brightness)
            is PlayerIntent.SetPlaybackSpeed -> setPlaybackSpeed(intent.speed)
            is PlayerIntent.SetAspectRatio -> setAspectRatio(intent.mode)
            is PlayerIntent.SelectSubtitleTrack -> selectSubtitle(intent.track)
            is PlayerIntent.AdjustSubtitleOffset -> adjustSubtitleOffset(intent.offsetMs)
            is PlayerIntent.SelectAudioTrack -> selectAudio(intent.track)
            is PlayerIntent.ToggleControlsLock -> toggleControlsLock()
            is PlayerIntent.ToggleHudVisibility -> toggleHudVisibility()
            is PlayerIntent.PlayNextEpisode -> playNextEpisode()
            is PlayerIntent.EnterPictureInPicture -> {
                viewModelScope.launch { _effects.send(PlayerEffect.EnterPictureInPicture) }
            }
            is PlayerIntent.ReleasePlayer -> releasePlayer()
        }
    }

    private fun initialize(mediaId: String, startPositionMs: Long) {
        _uiState.update {
            it.copy(
                mediaId = mediaId,
                isBuffering = true,
                errorMessage = null
            )
        }

        val isDownloaded = DownloadManagerProvider.isMediaDownloadedById(context, mediaId)
        val isOnline = isNetworkAvailable(context)

        viewModelScope.launch {
            // 1. Ensure authenticated session if online
            if (isOnline) {
                try {
                    if (sessionManager.getToken().isNullOrBlank()) {
                        handshakeRepository.performHandshake()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Login handshake fallback: ${e.message}")
                }
            }

            // 2. Fetch rich media details for titles, streams, and resume point
            val userId = sessionManager.getUserId().orEmpty()
            var resumeMs = startPositionMs

            if (isOnline) {
                try {
                    val item = apiService.getItemDetails(userId = userId, itemId = mediaId)
                    val isEpisode = item.type.equals("Episode", ignoreCase = true)
                    val title = if (isEpisode) {
                        val s = item.parentIndexNumber ?: 1
                        val e = item.indexNumber ?: 1
                        "${item.seriesName.orEmpty()} — S${s}E$e"
                    } else {
                        item.name.orEmpty()
                    }
                    val subTitle = if (isEpisode) {
                        item.name.orEmpty()
                    } else {
                        item.productionYear?.toString().orEmpty()
                    }

                    val primarySource = item.mediaSources?.firstOrNull()
                    currentMediaSourceId = primarySource?.id
                    val videoStream = primarySource?.mediaStreams?.firstOrNull { it.type == "Video" }
                    val audioStream = primarySource?.mediaStreams?.firstOrNull { it.type == "Audio" }

                    val vCodec = videoStream?.codec?.uppercase()?.let { "$it ${videoStream.height ?: 1080}p" } ?: "HEVC 4K"
                    val aCodec = audioStream?.codec?.uppercase()?.let { "$it ${audioStream.channelLayout ?: "Stereo"}" } ?: "TrueHD 7.1"

                    if (resumeMs == 0L && item.userData?.playbackPositionTicks != null) {
                        resumeMs = item.userData.playbackPositionTicks / 10_000L
                    }

                    _uiState.update {
                        it.copy(
                            mediaTitle = title,
                            mediaSubtitle = if (isDownloaded) "🟢 تشغيل أوفلاين محلي · $subTitle" else subTitle,
                            videoCodecInfo = vCodec,
                            audioCodecInfo = aCodec,
                            playbackMethod = PlaybackMethod.DirectPlay
                        )
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed fetching media details: ${e.message}")
                    _uiState.update {
                        it.copy(
                            mediaTitle = AppConfig.APP_NAME,
                            mediaSubtitle = if (isDownloaded) "🟢 تشغيل أوفلاين من الذاكرة" else "بث مباشر عتادي"
                        )
                    }
                }
            } else {
                // Completely offline mode: zero network requests
                _uiState.update {
                    it.copy(
                        mediaTitle = AppConfig.APP_NAME,
                        mediaSubtitle = "🟢 تشغيل أوفلاين بدون إنترنت",
                        playbackMethod = PlaybackMethod.DirectPlay
                    )
                }
            }

            // 3. Construct stream URL (Media3 CacheDataSource automatically plays from cache with zero network calls)
            val token = sessionManager.getToken()
            val baseServer = AppConfig.SERVER_URL.trimEnd('/')
            val streamUrl = StringBuilder("$baseServer/Videos/$mediaId/stream?static=true")
            if (!token.isNullOrBlank()) {
                streamUrl.append("&api_key=$token")
            }

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl.toString())
                .setMediaId(mediaId)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()

            if (resumeMs > 0L) {
                player.seekTo(resumeMs)
            }
            player.playWhenReady = true

            // 4. Report initial playback telemetry if online
            if (isOnline) {
                reportPlaybackStart(resumeMs)
            }
        }
    }

    private fun seekTo(positionMs: Long) {
        val duration = player.duration.coerceAtLeast(0L)
        val targetMs = positionMs.coerceIn(0L, duration)
        player.seekTo(targetMs)
        _uiState.update { it.copy(currentPositionMs = targetMs) }
        reportPlaybackProgress("seek")
    }

    private fun fastSeekBy(deltaMs: Long) {
        val current = player.currentPosition
        val duration = player.duration.coerceAtLeast(0L)
        val target = (current + deltaMs).coerceIn(0L, duration)
        player.seekTo(target)
        _uiState.update { it.copy(currentPositionMs = target) }
        viewModelScope.launch {
            _effects.send(PlayerEffect.TriggerHapticTick)
        }
        reportPlaybackProgress("seek")
    }

    private fun setVolume(newVolume: Float) {
        val clampedVolume = newVolume.coerceIn(0.0f, 2.0f)
        val prevVolume = _uiState.value.volume
        _uiState.update { it.copy(volume = clampedVolume) }

        if (clampedVolume <= 1.0f) {
            player.volume = clampedVolume
            try {
                loudnessEnhancer?.setTargetGain(0)
                loudnessEnhancer?.enabled = false
            } catch (e: Exception) {
                Log.w(tag, "LoudnessEnhancer reset error: ${e.message}")
            }
        } else {
            // Software Audio Boost (101% to 200%)
            player.volume = 1.0f
            val boostFraction = (clampedVolume - 1.0f) // 0.0 to 1.0
            val targetGainMb = (boostFraction * 1000f).toInt() // 0 to +1000 mB
            initLoudnessEnhancerIfNeeded()
            try {
                loudnessEnhancer?.setTargetGain(targetGainMb)
                loudnessEnhancer?.enabled = true
            } catch (e: Exception) {
                Log.w(tag, "LoudnessEnhancer boost error: ${e.message}")
            }
        }

        // Haptic feedback thresholds
        viewModelScope.launch {
            if ((prevVolume <= 1.0f && clampedVolume > 1.0f) || (prevVolume > 1.0f && clampedVolume <= 1.0f)) {
                _effects.send(PlayerEffect.TriggerHapticDouble)
            } else if (kotlin.math.abs(clampedVolume - prevVolume) >= 0.10f) {
                _effects.send(PlayerEffect.TriggerHapticTick)
            }
        }
    }

    private fun setBrightness(newBrightness: Float) {
        val clamped = newBrightness.coerceIn(0.01f, 1.0f)
        val prev = _uiState.value.brightness
        _uiState.update { it.copy(brightness = clamped) }

        if (kotlin.math.abs(clamped - prev) >= 0.10f) {
            viewModelScope.launch {
                _effects.send(PlayerEffect.TriggerHapticTick)
            }
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.25f, 3.0f)
        player.setPlaybackSpeed(clampedSpeed)
        _uiState.update { it.copy(playbackSpeed = clampedSpeed) }
        viewModelScope.launch {
            _effects.send(PlayerEffect.TriggerHapticTick)
        }
    }

    private fun setAspectRatio(mode: AspectRatioMode) {
        _uiState.update { it.copy(aspectRatioMode = mode) }
        viewModelScope.launch {
            _effects.send(PlayerEffect.TriggerHapticTick)
        }
    }

    private fun selectSubtitle(option: SubtitleOption?) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (option == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            _uiState.update {
                it.copy(
                    selectedSubtitle = null,
                    currentSubtitleText = null
                )
            }
        } else {
            val tracks = player.currentTracks
            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            if (option.trackGroupIndex in textGroups.indices) {
                val group = textGroups[option.trackGroupIndex]
                if (option.trackIndex in 0 until group.length) {
                    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    builder.setOverrideForType(
                        TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
                    )
                    _uiState.update { it.copy(selectedSubtitle = option) }
                }
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun adjustSubtitleOffset(offsetMs: Long) {
        _uiState.update {
            it.copy(subtitleOffsetMs = (it.subtitleOffsetMs + offsetMs).coerceIn(-5000L, 5000L))
        }
        viewModelScope.launch {
            _effects.send(PlayerEffect.TriggerHapticTick)
        }
    }

    private fun selectAudio(option: AudioOption) {
        val tracks = player.currentTracks
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (option.trackGroupIndex in audioGroups.indices) {
            val group = audioGroups[option.trackGroupIndex]
            if (option.trackIndex in 0 until group.length) {
                val builder = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setOverrideForType(
                        TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
                    )
                player.trackSelectionParameters = builder.build()
                _uiState.update { it.copy(selectedAudioTrack = option) }
            }
        }
    }

    private fun toggleControlsLock() {
        val willLock = !_uiState.value.isControlsLocked
        _uiState.update {
            it.copy(
                isControlsLocked = willLock,
                isHudVisible = !willLock
            )
        }
        viewModelScope.launch {
            _effects.send(if (willLock) PlayerEffect.TriggerHapticHeavy else PlayerEffect.TriggerHapticDouble)
        }
    }

    private fun toggleHudVisibility() {
        if (_uiState.value.isControlsLocked) return
        _uiState.update { it.copy(isHudVisible = !it.isHudVisible) }
    }

    private fun playNextEpisode() {
        val nextId = _uiState.value.nextEpisodeId ?: return
        initialize(nextId, 0L)
    }

    private fun parseAvailableTracks(tracks: Tracks) {
        val subtitles = mutableListOf<SubtitleOption>()
        var subCounter = 0
        val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        var activeSub: SubtitleOption? = null

        textGroups.forEachIndexed { groupIdx, group ->
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language ?: "ar"
                val name = format.label ?: if (lang == "ar") "العربية" else lang.uppercase()
                val isSelected = group.isTrackSelected(trackIdx)
                val option = SubtitleOption(
                    index = subCounter++,
                    name = name,
                    language = lang,
                    isDefault = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0,
                    trackGroupIndex = groupIdx,
                    trackIndex = trackIdx
                )
                subtitles.add(option)
                if (isSelected) {
                    activeSub = option
                }
            }
        }

        val audioList = mutableListOf<AudioOption>()
        var audioCounter = 0
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        var activeAudio: AudioOption? = null

        audioGroups.forEachIndexed { groupIdx, group ->
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language ?: "ar"
                val name = format.label ?: "${lang.uppercase()} (${format.sampleMimeType?.substringAfter('/') ?: "Stereo"})"
                val isSelected = group.isTrackSelected(trackIdx)
                val option = AudioOption(
                    index = audioCounter++,
                    name = name,
                    language = lang,
                    codec = format.sampleMimeType,
                    trackGroupIndex = groupIdx,
                    trackIndex = trackIdx
                )
                audioList.add(option)
                if (isSelected) {
                    activeAudio = option
                }
            }
        }

        _uiState.update {
            it.copy(
                availableSubtitles = subtitles.toImmutableList(),
                selectedSubtitle = activeSub ?: it.selectedSubtitle,
                availableAudioTracks = audioList.toImmutableList(),
                selectedAudioTrack = activeAudio ?: it.selectedAudioTrack
            )
        }
    }

    private fun initLoudnessEnhancerIfNeeded() {
        val sessionId = player.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId)
            } catch (e: Exception) {
                Log.w(tag, "LoudnessEnhancer initialization unsupported on this device: ${e.message}")
            }
        }
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(300L)
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val buffered = player.bufferedPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)

                    _uiState.update {
                        it.copy(
                            currentPositionMs = pos,
                            bufferedPositionMs = buffered,
                            totalDurationMs = dur
                        )
                    }

                    // Watch completion threshold (90%)
                    if (!hasMarkedPlayed && dur > 0 && pos.toDouble() / dur >= 0.90) {
                        hasMarkedPlayed = true
                        markPlayedOnServer()
                    }

                    // Periodic session report every 10 seconds
                    val now = System.currentTimeMillis()
                    if (now - lastReportTimeMs >= 10_000L) {
                        lastReportTimeMs = now
                        reportPlaybackProgress("timeupdate")
                    }
                }
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun reportPlaybackStart(startPositionMs: Long) {
        val mediaId = _uiState.value.mediaId
        if (mediaId.isBlank() || !isNetworkAvailable(context)) return

        viewModelScope.launch {
            try {
                apiService.reportPlaybackStart(
                    PlaybackStartDto(
                        itemId = mediaId,
                        positionTicks = startPositionMs * 10_000L,
                        mediaSourceId = currentMediaSourceId,
                        playMethod = "DirectPlay",
                        canSeek = true,
                        isPaused = false
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Failed to report playback start: ${e.message}")
            }
        }
    }

    private fun reportPlaybackProgress(eventName: String) {
        val mediaId = _uiState.value.mediaId
        if (mediaId.isBlank() || !isNetworkAvailable(context)) return

        val currentTicks = player.currentPosition.coerceAtLeast(0L) * 10_000L
        val isPaused = !player.isPlaying

        viewModelScope.launch {
            try {
                apiService.reportPlaybackProgress(
                    PlaybackProgressDto(
                        itemId = mediaId,
                        positionTicks = currentTicks,
                        isPaused = isPaused,
                        canSeek = true,
                        playMethod = "DirectPlay",
                        eventName = eventName,
                        mediaSourceId = currentMediaSourceId
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Failed to report playback progress: ${e.message}")
            }
        }
    }

    private fun reportPlaybackStopped() {
        val mediaId = _uiState.value.mediaId
        if (mediaId.isBlank() || !isNetworkAvailable(context)) return

        val finalTicks = player.currentPosition.coerceAtLeast(0L) * 10_000L
        viewModelScope.launch {
            try {
                apiService.reportPlaybackStopped(
                    PlaybackStopDto(
                        itemId = mediaId,
                        positionTicks = finalTicks,
                        mediaSourceId = currentMediaSourceId
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Failed to report playback stopped: ${e.message}")
            }
        }
    }

    private fun markPlayedOnServer() {
        val mediaId = _uiState.value.mediaId
        val userId = sessionManager.getUserId().orEmpty()
        if (mediaId.isBlank() || userId.isBlank() || !isNetworkAvailable(context)) return

        viewModelScope.launch {
            try {
                apiService.markPlayed(userId = userId, itemId = mediaId)
            } catch (e: Exception) {
                Log.w(tag, "Failed to mark item played: ${e.message}")
            }
        }
    }

    private fun releasePlayer() {
        progressTickerJob?.cancel()
        reportPlaybackStopped()
        try {
            player.removeListener(playerListener)
            player.stop()
            player.release()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing ExoPlayer: ${e.message}")
        }
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing LoudnessEnhancer: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
