package org.jellyfin.mobile.presentation.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.mobile.core.download.DownloadManagerProvider
import org.jellyfin.mobile.domain.model.DownloadsTab
import org.jellyfin.mobile.domain.repository.DownloadsRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository

/**
 * DownloadsViewModel manages offline media vault states, storage metrics,
 * and user interactions adhering to MVI architecture.
 * Strictly adheres to docs/screens/06_DOWNLOADS_OFFLINE.md.
 */
class DownloadsViewModel(
    private val downloadsRepository: DownloadsRepository,
    private val localProfileRepository: LocalProfileRepository? = null,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DownloadsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeDownloadsAndStorage()
    }

    private fun observeDownloadsAndStorage() {
        viewModelScope.launch {
            downloadsRepository.getDownloadedItems()
                .catch { error ->
                    _effects.send(DownloadsEffect.ShowToast(error.localizedMessage ?: "فشل في قراءة التنزيلات المحفوظة"))
                }
                .collect { allItems ->
                    val storage = runCatching {
                        downloadsRepository.getStorageBreakdown()
                    }.getOrDefault(
                        org.jellyfin.mobile.domain.model.StorageBreakdown(0L, 0L, 0L, 0L)
                    )

                    val movies = allItems.filter { it.mediaType.equals("Movie", ignoreCase = true) }
                    val series = allItems.filter {
                        it.mediaType.equals("Episode", ignoreCase = true) || it.mediaType.equals("Series", ignoreCase = true)
                    }

                    val isStorageLow = storage.freeGb < 5.0

                    _uiState.value = _uiState.value.copy(
                        downloadedMovies = movies.toImmutableList(),
                        downloadedSeries = series.toImmutableList(),
                        storageBreakdown = storage,
                        isStorageLowWarning = isStorageLow,
                        isDeleting = false
                    )
                }
        }
    }

    fun handleIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.LoadDownloadsData, is DownloadsIntent.RefreshStorage -> {
                refreshData()
            }
            is DownloadsIntent.SetFilterTab -> {
                _uiState.value = _uiState.value.copy(selectedTab = intent.tab)
            }
            is DownloadsIntent.ToggleWifiOnly -> {
                val current = _uiState.value.isWifiOnlyEnabled
                _uiState.value = _uiState.value.copy(isWifiOnlyEnabled = !current)
            }
            is DownloadsIntent.PauseDownload -> {
                context?.let { DownloadManagerProvider.sendPauseDownloads(it) }
                viewModelScope.launch {
                    _effects.send(DownloadsEffect.ShowToast("تم إيقاف التنزيل مؤقتاً"))
                }
            }
            is DownloadsIntent.ResumeDownload -> {
                context?.let { DownloadManagerProvider.sendResumeDownloads(it) }
                viewModelScope.launch {
                    _effects.send(DownloadsEffect.ShowToast("جارٍ استئناف التنزيل..."))
                }
            }
            is DownloadsIntent.CancelDownload -> {
                context?.let { DownloadManagerProvider.sendRemoveDownload(it, intent.mediaId) }
                viewModelScope.launch {
                    _effects.send(DownloadsEffect.ShowToast("تم إلغاء التنزيل"))
                }
            }
            is DownloadsIntent.DeleteDownloadedItem -> {
                deleteItem(intent.mediaId)
            }
            is DownloadsIntent.DeleteAllWatched -> {
                deleteWatchedOnly()
            }
            is DownloadsIntent.DeleteAllDownloads -> {
                deleteAll()
            }
            is DownloadsIntent.PlayOfflineMedia -> {
                viewModelScope.launch {
                    _effects.send(DownloadsEffect.LaunchPlayer(mediaId = intent.mediaId, positionMs = 0L))
                }
            }
        }
    }

    fun processIntent(intent: DownloadsIntent) {
        handleIntent(intent)
    }

    private fun deleteItem(mediaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            downloadsRepository.deleteDownloadedItem(mediaId)
                .onSuccess {
                    _effects.send(DownloadsEffect.ShowToast("تم حذف الملف بنجاح وتوفير المساحة"))
                }
                .onFailure {
                    _effects.send(DownloadsEffect.ShowToast("تعذر حذف الملف"))
                }

            refreshData()
        }
    }

    private fun deleteWatchedOnly() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            val allItems = _uiState.value.downloadedMovies + _uiState.value.downloadedSeries
            var deletedCount = 0

            for (item in allItems) {
                val isWatched = localProfileRepository?.isWatched(item.id) ?: false
                if (isWatched) {
                    downloadsRepository.deleteDownloadedItem(item.id)
                    deletedCount++
                }
            }

            if (deletedCount > 0) {
                _effects.send(DownloadsEffect.ShowToast("تم حذف $deletedCount من الأعمال المشاهدة وتوفير المساحة"))
            } else {
                _effects.send(DownloadsEffect.ShowToast("لا توجد أعمال مكتملة المشاهدة لحذفها"))
            }

            refreshData()
        }
    }

    private fun deleteAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            downloadsRepository.clearAllDownloads()
                .onSuccess {
                    _effects.send(DownloadsEffect.ShowToast("تم مسح كافة ملفات التنزيلات"))
                }
                .onFailure {
                    _effects.send(DownloadsEffect.ShowToast("فشل مسح التنزيلات"))
                }

            refreshData()
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            val storage = downloadsRepository.getStorageBreakdown()
            _uiState.value = _uiState.value.copy(
                storageBreakdown = storage,
                isStorageLowWarning = storage.freeGb < 5.0,
                isDeleting = false
            )
        }
    }
}
