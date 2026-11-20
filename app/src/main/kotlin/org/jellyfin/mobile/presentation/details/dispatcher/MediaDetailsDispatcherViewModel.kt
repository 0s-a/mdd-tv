package org.jellyfin.mobile.presentation.details.dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.repository.HandshakeRepository

/**
 * MediaDetailsDispatcherViewModel resolves the media type polymorphically
 * to route to the appropriate dedicated details screen (Movie, Series, BoxSet, MusicAlbum).
 * Strictly zero emojis.
 */
class MediaDetailsDispatcherViewModel(
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val handshakeRepository: HandshakeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaDetailsDispatcherUiState>(MediaDetailsDispatcherUiState.Loading)
    val uiState: StateFlow<MediaDetailsDispatcherUiState> = _uiState.asStateFlow()

    fun processIntent(intent: MediaDetailsDispatcherIntent) {
        when (intent) {
            is MediaDetailsDispatcherIntent.ResolveType -> resolveType(intent.mediaId, intent.explicitType)
            is MediaDetailsDispatcherIntent.Retry -> resolveType(intent.mediaId, intent.explicitType)
        }
    }

    private fun resolveType(mediaId: String, explicitType: String?) {
        val mappedExplicit = explicitType?.let { mapTypeStringToResolved(it) }
        if (mappedExplicit != null) {
            _uiState.value = MediaDetailsDispatcherUiState.Resolved(mappedExplicit)
            return
        }

        viewModelScope.launch {
            _uiState.value = MediaDetailsDispatcherUiState.Loading
            try {
                val userId = sessionManager.getUserId()
                    ?: handshakeRepository.currentSession.value?.userId
                    ?: handshakeRepository.performHandshake().getOrNull()?.userId

                if (userId.isNullOrBlank()) {
                    _uiState.value = MediaDetailsDispatcherUiState.Error("جلسة المستخدم غير متوفرة")
                    return@launch
                }

                val itemDto = apiService.getItemDetails(userId = userId, itemId = mediaId)
                val resolvedType = mapTypeStringToResolved(itemDto.type) ?: ResolvedMediaType.MOVIE
                _uiState.value = MediaDetailsDispatcherUiState.Resolved(resolvedType)
            } catch (e: Exception) {
                _uiState.value = MediaDetailsDispatcherUiState.Error(
                    e.localizedMessage ?: "فشل في تحديد نوع الوسائط"
                )
            }
        }
    }

    private fun mapTypeStringToResolved(type: String?): ResolvedMediaType? {
        if (type == null) return null
        return when (type.lowercase()) {
            "movie", "video", "trailer", "episode" -> ResolvedMediaType.MOVIE
            "series", "tvshow", "season" -> ResolvedMediaType.SERIES
            "boxset", "collectionfolder", "playlist" -> ResolvedMediaType.BOXSET
            "musicalbum", "musicartist", "audio" -> ResolvedMediaType.MUSIC_ALBUM
            else -> null
        }
    }
}
