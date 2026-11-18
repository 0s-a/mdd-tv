package org.jellyfin.mobile.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

val DefaultAlphabetList: ImmutableList<Char> = persistentListOf(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#'
)

/**
 * AlphabetScrubber provides fast alphabetical navigation along the trailing edge.
 * Strictly adheres to docs/screens/02_LIBRARY_BROWSER.md Section 4:
 * - Vertical alphabet list (A..Z + #).
 * - Tactile haptic feedback on letter change.
 * - Floating magnifying bubble displaying the current letter.
 */
@Composable
fun AlphabetScrubber(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
    letters: ImmutableList<Char> = DefaultAlphabetList
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var activeChar by remember { mutableStateOf<Char?>(null) }
    var columnHeightPx by remember { mutableFloatStateOf(0f) }
    var touchYPx by remember { mutableFloatStateOf(0f) }

    fun selectLetterFromY(y: Float) {
        if (columnHeightPx <= 0f || letters.isEmpty()) return
        val clampedY = y.coerceIn(0f, columnHeightPx - 1f)
        val index = ((clampedY / columnHeightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val selected = letters[index]

        if (selected != activeChar) {
            activeChar = selected
            touchYPx = clampedY
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterSelected(selected)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = JellyfinDimens.SpaceLarge)
            .semantics {
                contentDescription = "شريط التمرير الأبجدي السريع"
            }
    ) {
        // 1. Floating Magnified Letter Bubble
        AnimatedVisibility(
            visible = isDragging && activeChar != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val offsetY = (touchYPx - with(density) { 32.dp.toPx() }).toInt()
                    IntOffset(
                        x = with(density) { (-48).dp.roundToPx() },
                        y = offsetY.coerceAtLeast(0)
                    )
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = JellyfinDimens.ElevationMedium,
                        shape = CircleShape,
                        spotColor = JellyfinColors.PrimaryIndigo
                    )
                    .clip(CircleShape)
                    .glassmorphism(
                        backgroundColor = JellyfinColors.SurfaceDark.copy(alpha = 0.9f),
                        borderColor = JellyfinColors.PrimaryIndigo,
                        cornerRadius = 28.dp,
                        borderWidth = JellyfinDimens.BorderMedium
                    )
            ) {
                Text(
                    text = activeChar?.toString() ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.PrimaryIndigo
                )
            }
        }

        // 2. Vertical Alphabet Track
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(28.dp)
                .fillMaxHeight(0.85f)
                .clip(CircleShape)
                .glassmorphism(
                    backgroundColor = JellyfinColors.SurfaceGlass.copy(alpha = 0.35f),
                    borderColor = JellyfinColors.BorderGlass,
                    cornerRadius = JellyfinDimens.RadiusPill
                )
                .padding(vertical = JellyfinDimens.SpaceExtraSmall)
                .onGloballyPositioned { coordinates ->
                    columnHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(letters) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            selectLetterFromY(offset.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            activeChar = null
                        },
                        onDragCancel = {
                            isDragging = false
                            activeChar = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            selectLetterFromY(change.position.y)
                        }
                    )
                }
                .pointerInput(letters) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            selectLetterFromY(offset.y)
                            tryAwaitRelease()
                            isDragging = false
                            activeChar = null
                        }
                    )
                }
        ) {
            letters.forEach { char ->
                val isCurrent = char == activeChar
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) JellyfinColors.PrimaryIndigo else JellyfinColors.TextSecondary
                )
            }
        }
    }
}
