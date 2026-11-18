package org.jellyfin.mobile.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.mobile.domain.model.AvatarCatalog
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileBottomSheet(
    currentName: String,
    currentAvatarKey: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(currentName) { mutableStateOf(currentName) }
    var selectedAvatar by remember(currentAvatarKey) { mutableStateOf(currentAvatarKey) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JellyfinColors.SurfaceDark,
        shape = RoundedCornerShape(
            topStart = JellyfinDimens.RadiusLarge,
            topEnd = JellyfinDimens.RadiusLarge
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = JellyfinDimens.SpaceLarge)
                .padding(bottom = JellyfinDimens.SpaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تعديل الملف الشخصي",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

            // Name field
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "الاسم الشخصي",
                    style = MaterialTheme.typography.titleSmall,
                    color = JellyfinColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
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
                    text = "الصورة الرمزية",
                    style = MaterialTheme.typography.titleSmall,
                    color = JellyfinColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                    maxItemsInEachRow = 4
                ) {
                    AvatarCatalog.avatars.forEach { avatar ->
                        val isSelected = selectedAvatar == avatar.key
                        val borderModifier = if (isSelected) {
                            Modifier.border(2.dp, JellyfinColors.PrimaryIndigo, CircleShape)
                        } else {
                            Modifier.border(1.dp, JellyfinColors.BorderGlass, CircleShape)
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .then(borderModifier)
                                .background(
                                    if (isSelected) JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f)
                                    else JellyfinColors.SurfaceGlass
                                )
                                .clickable { selectedAvatar = avatar.key }
                                .semantics {
                                    role = Role.RadioButton
                                    this.selected = isSelected
                                    contentDescription = "الصورة الرمزية ${avatar.label}"
                                }
                        ) {
                            Text(
                                text = avatar.emoji,
                                fontSize = 26.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceExtraLarge))

            Button(
                onClick = { onSave(name, selectedAvatar) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.PrimaryIndigo,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "حفظ التغييرات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
