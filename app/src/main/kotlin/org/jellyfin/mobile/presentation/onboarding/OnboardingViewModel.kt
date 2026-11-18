package org.jellyfin.mobile.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.repository.LocalProfileRepository

data class OnboardingUiState(
    val displayName: String = "",
    val selectedAvatarKey: String = "avatar_1",
    val isLoading: Boolean = false
)

sealed interface OnboardingEffect {
    data object NavigateToHome : OnboardingEffect
}

class OnboardingViewModel(
    private val localProfileRepository: LocalProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun updateName(name: String) {
        if (name.length <= 20) {
            _uiState.update { it.copy(displayName = name) }
        }
    }

    fun selectAvatar(avatarKey: String) {
        _uiState.update { it.copy(selectedAvatarKey = avatarKey) }
    }

    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val finalName = _uiState.value.displayName.trim().ifBlank { "مشاهد مدد" }
            val finalAvatar = _uiState.value.selectedAvatarKey.ifBlank { "avatar_1" }
            localProfileRepository.createProfile(displayName = finalName, avatarKey = finalAvatar)
            _uiState.update { it.copy(isLoading = false) }
            _effects.send(OnboardingEffect.NavigateToHome)
        }
    }

    fun skip() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            localProfileRepository.createProfile(displayName = "مشاهد مدد", avatarKey = "avatar_1")
            _uiState.update { it.copy(isLoading = false) }
            _effects.send(OnboardingEffect.NavigateToHome)
        }
    }
}
