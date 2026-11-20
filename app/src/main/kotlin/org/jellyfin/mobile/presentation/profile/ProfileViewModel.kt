package org.jellyfin.mobile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity
import org.jellyfin.mobile.domain.model.AvatarCatalog
import org.jellyfin.mobile.domain.repository.LocalProfileRepository

enum class ProfileTab {
    FAVORITES,
    WATCHED_ARCHIVE,
    LOCAL_STORAGE
}

data class ProfileUiState(
    val userProfile: UserProfileEntity? = null,
    val selectedTab: ProfileTab = ProfileTab.FAVORITES,
    val favorites: List<LocalFavoriteEntity> = emptyList(),
    val watchedArchive: List<WatchedArchiveEntity> = emptyList(),
    val isEditSheetVisible: Boolean = false,
    val isLoading: Boolean = false
) {
    val avatarEmoji: String get() = AvatarCatalog.getEmoji(userProfile?.avatarKey)
    val displayName: String get() = userProfile?.displayName ?: "مشاهد مدد"
}

class ProfileViewModel(
    private val localProfileRepository: LocalProfileRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ProfileTab.FAVORITES)
    private val _isEditSheetVisible = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        localProfileRepository.observeProfile(),
        localProfileRepository.observeFavorites(),
        localProfileRepository.observeArchive(),
        _selectedTab,
        _isEditSheetVisible
    ) { profile, favorites, archive, tab, isEditVisible ->
        ProfileUiState(
            userProfile = profile,
            selectedTab = tab,
            favorites = favorites,
            watchedArchive = archive,
            isEditSheetVisible = isEditVisible,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

    fun selectTab(tab: ProfileTab) {
        _selectedTab.update { tab }
    }

    fun setEditSheetVisible(visible: Boolean) {
        _isEditSheetVisible.update { visible }
    }

    fun updateProfile(displayName: String, avatarKey: String) {
        viewModelScope.launch {
            val finalName = displayName.trim().ifBlank { "مشاهد مدد" }
            val finalAvatar = avatarKey.ifBlank { "avatar_1" }
            localProfileRepository.updateProfile(finalName, finalAvatar)
            _isEditSheetVisible.update { false }
        }
    }

    fun removeFavorite(itemId: String) {
        viewModelScope.launch {
            localProfileRepository.removeFavorite(itemId)
        }
    }

    fun removeFromArchive(itemId: String) {
        viewModelScope.launch {
            localProfileRepository.removeFromArchive(itemId)
        }
    }
}
