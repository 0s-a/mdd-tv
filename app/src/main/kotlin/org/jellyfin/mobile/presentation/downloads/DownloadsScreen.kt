package org.jellyfin.mobile.presentation.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.presentation.core.components.FloatingFrostedTopBar
import org.jellyfin.mobile.presentation.downloads.components.ActiveDownloadsList
import org.jellyfin.mobile.presentation.downloads.components.DownloadedMediaTabs
import org.jellyfin.mobile.presentation.downloads.components.StorageGaugeCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * DownloadsScreen implements the Offline Media Vault and Download Manager.
 * Strictly adheres to docs/screens/06_DOWNLOADS_OFFLINE.md:
 * - OLED True Black (#000000) backdrop.
 * - Interactive Frosted Storage Gauge (Jellyfin vs System/Apps vs Free space).
 * - Active Foreground Downloads list with real-time transfer metrics and action buttons.
 * - Saved Media Tabs (Movies, Series, Storage Management) with offline playback and instant deletion.
 * - Minimum 48dp touch targets and TalkBack accessibility semantics.
 */
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onLaunchPlayer: (mediaId: String, positionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is DownloadsEffect.LaunchPlayer -> {
                    onLaunchPlayer(effect.mediaId, effect.positionMs)
                }
                is DownloadsEffect.PlayOffline -> {
                    onLaunchPlayer(effect.mediaId, 0L)
                }
                is DownloadsEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        withDismissAction = true
                    )
                }
                is DownloadsEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        withDismissAction = true
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = backgroundColor,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = 76.dp,
                    bottom = 110.dp // Prevent bottom navigation bar overlap
                ),
                verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceLarge),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Storage Gauge Card
                item(key = "storage_gauge") {
                    StorageGaugeCard(
                        storageInfo = uiState.storageInfo,
                        modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
                    )
                }

                // 2. Storage Low Warning (if applicable)
                if (uiState.isStorageLowWarning || uiState.storageInfo.freeGb < 5.0) {
                    item(key = "storage_low_warning") {
                        StorageLowWarningBanner(
                            freeGb = uiState.storageInfo.freeGb,
                            modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
                        )
                    }
                }

                // 3. Active Downloads List (Foreground Queue)
                if (uiState.activeDownloads.isNotEmpty()) {
                    item(key = "active_downloads_section") {
                        ActiveDownloadsList(
                            tasks = uiState.activeDownloads,
                            onPauseClick = { mediaId ->
                                viewModel.handleIntent(DownloadsIntent.PauseDownload(mediaId))
                            },
                            onResumeClick = { mediaId ->
                                viewModel.handleIntent(DownloadsIntent.ResumeDownload(mediaId))
                            },
                            onCancelClick = { mediaId ->
                                viewModel.handleIntent(DownloadsIntent.CancelDownload(mediaId))
                            },
                            modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
                        )
                    }
                }

                // 4. Downloaded Media Tabs & Items
                item(key = "downloaded_media_tabs") {
                    val allDownloadedItems = remember(uiState.downloadedMovies, uiState.downloadedSeries) {
                        (uiState.downloadedMovies + uiState.downloadedSeries).toImmutableList()
                    }

                    DownloadedMediaTabs(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { tab ->
                            viewModel.handleIntent(DownloadsIntent.SetFilterTab(tab))
                        },
                        items = allDownloadedItems,
                        onPlayOfflineClick = { mediaId ->
                            viewModel.handleIntent(DownloadsIntent.PlayOfflineMedia(mediaId))
                        },
                        onDeleteItemClick = { mediaId ->
                            viewModel.handleIntent(DownloadsIntent.DeleteDownloadedItem(mediaId))
                        },
                        onDeleteAllWatchedClick = {
                            viewModel.handleIntent(DownloadsIntent.DeleteAllWatched)
                        }
                    )
                }
            }

            // Floating Frosted Top Bar (Detached / بشكل غير متصل)
            DownloadsFloatingTopBar(
                isWifiOnly = uiState.isWifiOnlyEnabled,
                onToggleWifiOnly = { viewModel.handleIntent(DownloadsIntent.ToggleWifiOnly) },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * DownloadsFloatingTopBar provides a detached glassmorphic top header matching the bottom navigation pill.
 */
@Composable
private fun DownloadsFloatingTopBar(
    isWifiOnly: Boolean,
    onToggleWifiOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingFrostedTopBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    tint = JellyfinColors.PrimaryIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "التنزيلات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
            }

            // Wi-Fi Only Guard Chip
            val wifiDesc = if (isWifiOnly) {
                "وضع التنزيل عبر الواي فاي فقط مفعل، اضغط للتبديل"
            } else {
                "وضع التنزيل عبر شبكة البيانات مفعل، اضغط للتبديل"
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .glassmorphism(
                        backgroundColor = if (isWifiOnly) {
                            JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f)
                        } else {
                            JellyfinColors.SurfaceGlass
                        },
                        borderColor = if (isWifiOnly) {
                            JellyfinColors.TechSky
                        } else {
                            JellyfinColors.BorderGlass
                        },
                        cornerRadius = JellyfinDimens.RadiusPill
                    )
                    .clickable(
                        role = Role.Switch,
                        onClick = onToggleWifiOnly
                    )
                    .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceExtraSmall)
                    .semantics(mergeDescendants = true) {
                        role = Role.Switch
                        contentDescription = wifiDesc
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
                ) {
                    Icon(
                        imageVector = if (isWifiOnly) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = if (isWifiOnly) JellyfinColors.TechSky else JellyfinColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isWifiOnly) "الواي فاي فقط" else "أي شبكة",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWifiOnly) JellyfinColors.TechSky else JellyfinColors.TextMuted
                    )
                }
            }
        }
    }
}

/**
 * StorageLowWarningBanner alerts user when disk space is critically low.
 */
@Composable
private fun StorageLowWarningBanner(
    freeGb: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.RatingGold.copy(alpha = 0.15f),
                borderColor = JellyfinColors.RatingGold.copy(alpha = 0.5f),
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .padding(JellyfinDimens.SpaceMedium)
            .semantics(mergeDescendants = true) {
                contentDescription = "تحذير: الذاكرة متبقي منها ${String.format("%.1f", freeGb)} جيجابايت فقط"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = JellyfinColors.RatingGold,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تنبيه: مساحة التخزين منخفضة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.RatingGold
                )
                Text(
                    text = "متبقي ${String.format("%.1f GB", freeGb)} فقط. يمكنك تحرير مساحة بحذف الأعمال المشاهدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextPrimary
                )
            }
        }
    }
}
