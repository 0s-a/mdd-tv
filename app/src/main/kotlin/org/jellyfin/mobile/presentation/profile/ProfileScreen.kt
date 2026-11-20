package org.jellyfin.mobile.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.CinemaChildTopBar
import org.jellyfin.mobile.presentation.main.LocalBottomClearance
import org.jellyfin.mobile.presentation.main.NavigationTab
import org.jellyfin.mobile.presentation.main.OnTabReselected
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoritesGridState = rememberLazyGridState()
    val watchedGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    OnTabReselected(NavigationTab.PROFILE) {
        coroutineScope.launch {
            when (uiState.selectedTab) {
                ProfileTab.FAVORITES -> favoritesGridState.animateScrollToItem(0)
                ProfileTab.WATCHED_ARCHIVE -> watchedGridState.animateScrollToItem(0)
                ProfileTab.LOCAL_STORAGE -> { /* No scroll needed for local storage */ }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        AmbientGlow(
            color = JellyfinColors.PrimaryIndigo.copy(alpha = 0.15f),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Unified Cinema TopBar
            CinemaChildTopBar(
                title = "الملف الشخصي",
                onBackClick = onNavigateBack
            )

            // Avatar & Name Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = JellyfinDimens.SpaceMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, JellyfinColors.PrimaryIndigo, CircleShape)
                            .background(JellyfinColors.SurfaceGlass)
                    ) {
                        Text(
                            text = uiState.avatarEmoji,
                            fontSize = 44.sp
                        )
                    }

                    // Edit button badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(JellyfinColors.PrimaryIndigo)
                            .clickable { viewModel.setEditSheetVisible(true) }
                            .semantics {
                                role = Role.Button
                                contentDescription = "تعديل الملف الشخصي"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = JellyfinColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                Text(
                    text = uiState.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
            }

            // Three Tabs: Favorites, Watched Archive, Local Storage Info
            val tabs = listOf(
                "المفضلة (${uiState.favorites.size})",
                "المشاهدات (${uiState.watchedArchive.size})",
                "بيانات التخزين"
            )
            val selectedTabIndex = when (uiState.selectedTab) {
                ProfileTab.FAVORITES -> 0
                ProfileTab.WATCHED_ARCHIVE -> 1
                ProfileTab.LOCAL_STORAGE -> 2
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = JellyfinColors.SurfaceDark,
                contentColor = JellyfinColors.PrimaryIndigo,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = JellyfinColors.PrimaryIndigo
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            viewModel.selectTab(
                                when (index) {
                                    0 -> ProfileTab.FAVORITES
                                    1 -> ProfileTab.WATCHED_ARCHIVE
                                    else -> ProfileTab.LOCAL_STORAGE
                                }
                            )
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            val baseServer = AppConfig.SERVER_URL.trimEnd('/')
            when (uiState.selectedTab) {
                ProfileTab.FAVORITES -> {
                    if (uiState.favorites.isEmpty()) {
                        EmptyListState(
                            message = "لا توجد عناصر مضافة إلى المفضلة بعد",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = favoritesGridState,
                            contentPadding = PaddingValues(
                                start = JellyfinDimens.SpaceMedium,
                                top = JellyfinDimens.SpaceMedium,
                                end = JellyfinDimens.SpaceMedium,
                                bottom = LocalBottomClearance.current + JellyfinDimens.SpaceMedium
                            ),
                            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                            verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.favorites, key = { it.itemId }) { item ->
                                val posterUrl = "$baseServer/Items/${item.itemId}/Images/Primary?fillWidth=300&quality=80"
                                ProfileMediaPosterCard(
                                    title = item.title,
                                    subTitle = item.releaseYear,
                                    posterUrl = posterUrl,
                                    onClick = { onNavigateToDetails(item.itemId) }
                                )
                            }
                        }
                    }
                }
                ProfileTab.WATCHED_ARCHIVE -> {
                    if (uiState.watchedArchive.isEmpty()) {
                        EmptyListState(
                            message = "لم تقم بإنهاء مشاهدة أي عمل بعد",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = watchedGridState,
                            contentPadding = PaddingValues(
                                start = JellyfinDimens.SpaceMedium,
                                top = JellyfinDimens.SpaceMedium,
                                end = JellyfinDimens.SpaceMedium,
                                bottom = LocalBottomClearance.current + JellyfinDimens.SpaceMedium
                            ),
                            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                            verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.watchedArchive, key = { it.itemId }) { item ->
                                val posterUrl = "$baseServer/Items/${item.itemId}/Images/Primary?fillWidth=300&quality=80"
                                ProfileMediaPosterCard(
                                    title = item.title,
                                    subTitle = item.mediaType,
                                    posterUrl = posterUrl,
                                    onClick = { onNavigateToDetails(item.itemId) }
                                )
                            }
                        }
                    }
                }
                ProfileTab.LOCAL_STORAGE -> {
                    LocalStorageInfoView(
                        displayName = uiState.displayName,
                        serverUrl = baseServer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Edit Profile Bottom Sheet
        if (uiState.isEditSheetVisible) {
            EditProfileBottomSheet(
                currentName = uiState.displayName,
                currentAvatarKey = uiState.userProfile?.avatarKey ?: "avatar_1",
                onSave = { newName, newAvatar ->
                    viewModel.updateProfile(newName, newAvatar)
                },
                onDismiss = { viewModel.setEditSheetVisible(false) }
            )
        }
    }
}

@Composable
private fun ProfileMediaPosterCard(
    title: String,
    subTitle: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .background(JellyfinColors.SurfaceDark)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (subTitle.isNotBlank()) {
            Text(
                text = subTitle,
                style = MaterialTheme.typography.labelSmall,
                color = JellyfinColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyListState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(JellyfinDimens.SpaceLarge)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = JellyfinColors.TextMuted,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = JellyfinColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocalStorageInfoView(
    displayName: String,
    serverUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(JellyfinDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium)
    ) {
        // Card 1: Download Service Status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .background(JellyfinColors.SurfaceDark)
                .border(1.dp, JellyfinColors.BorderGlass, RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .padding(JellyfinDimens.SpaceMedium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "حالة خدمة التنزيل والمحتوى المحلي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Text(
                    text = "تم تعطيل خدمة التنزيل مؤقتاً لتطوير محرك التخزين والترقية إلى معمارية التخزين المحلية الجديدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary
                )
            }
        }

        // Card 2: Local Storage Footprint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .background(JellyfinColors.SurfaceDark)
                .border(1.dp, JellyfinColors.BorderGlass, RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .padding(JellyfinDimens.SpaceMedium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "المساحة المشغولة محلياً",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Text(
                    text = "0 ميغابايت (لا توجد ملفات فيديو مخزنة حالياً في الذاكرة)",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TechSky
                )
            }
        }

        // Card 3: Account & Streaming Profile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .background(JellyfinColors.SurfaceDark)
                .border(1.dp, JellyfinColors.BorderGlass, RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .padding(JellyfinDimens.SpaceMedium)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "بيانات الحساب والسيرفر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Text(
                    text = "المستخدم: $displayName",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary
                )
                Text(
                    text = "السيرفر: $serverUrl",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary
                )
                Text(
                    text = "نمط التشغيل المعتمد: البث المباشر (Direct Stream)",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.SuccessEmerald
                )
            }
        }
    }
}
