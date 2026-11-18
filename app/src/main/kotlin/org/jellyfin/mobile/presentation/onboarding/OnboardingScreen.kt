package org.jellyfin.mobile.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.domain.model.AvatarCatalog
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is OnboardingEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        AmbientGlow(
            color = JellyfinColors.PrimaryIndigo.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = JellyfinDimens.SpaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

            Text(
                text = "🍿 ${AppConfig.APP_NAME}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceExtraSmall))

            Text(
                text = "سينما منزلية فائقة السرعة",
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceExtraLarge))

            // Name field
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "ما اسمك؟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = JellyfinColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { viewModel.updateName(it) },
                    placeholder = {
                        Text(
                            text = "اكتب اسمك هنا...",
                            color = JellyfinColors.TextMuted
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JellyfinColors.TextPrimary,
                        unfocusedTextColor = JellyfinColors.TextPrimary,
                        focusedContainerColor = JellyfinColors.SurfaceGlass,
                        unfocusedContainerColor = JellyfinColors.SurfaceGlass,
                        focusedBorderColor = JellyfinColors.PrimaryIndigo,
                        unfocusedBorderColor = JellyfinColors.BorderGlass,
                        cursorColor = JellyfinColors.PrimaryIndigo
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

            // Avatar selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "اختر صورتك الرمزية:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = JellyfinColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                    maxItemsInEachRow = 4
                ) {
                    AvatarCatalog.avatars.forEach { avatar ->
                        val isSelected = uiState.selectedAvatarKey == avatar.key
                        val borderModifier = if (isSelected) {
                            Modifier.border(2.dp, JellyfinColors.PrimaryIndigo, CircleShape)
                        } else {
                            Modifier.border(1.dp, JellyfinColors.BorderGlass, CircleShape)
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .then(borderModifier)
                                .background(
                                    if (isSelected) JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f)
                                    else JellyfinColors.SurfaceGlass
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectAvatar(avatar.key)
                                }
                                .semantics {
                                    role = Role.RadioButton
                                    this.selected = isSelected
                                    contentDescription = "الصورة الرمزية ${avatar.label}"
                                }
                        ) {
                            Text(
                                text = avatar.emoji,
                                fontSize = 30.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceExtraLarge))

            // Enter button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.submit()
                },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.PrimaryIndigo,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "✨ دخول صالة العرض",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

            // Skip button
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.skip()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Text(
                    text = "تخطي والدخول باسم \"مشاهد مدد\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JellyfinColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
        }
    }
}
