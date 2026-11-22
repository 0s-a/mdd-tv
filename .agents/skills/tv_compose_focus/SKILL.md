---
name: tv-compose-focus
description: >-
  Provides guidelines, best practices, and code patterns for Android TV D-Pad focus management,
  focusRestorer, TV LazyLists, 120Hz rendering, and Obsidian Design System focus states in Jetpack Compose.
  Use when building or debugging TV UI screens, focus transitions, remote control navigation, or list scrolling in mdd_tv.
---

# Android TV D-Pad Focus & Compose Architecture Guide

## Overview

This skill establishes the definitive engineering patterns for remote-control (D-Pad) focus navigation, focus memory, list scrolling performance, and Obsidian visual focus states for the `mdd_tv` client.

---

## 1. Core Principles for 10-Foot UI

1. **Non-Touch Navigation First:** Every interactive component must be reachable and controllable via Directional Pad (Up, Down, Left, Right, Center/Select).
2. **Predictable Focus Paths:** Focus must never be trapped, lost in empty space, or jump erratically across unrelated sections.
3. **Focus Memory:** When leaving a horizontal carousel or media row, returning focus to that row must restore focus to the previously selected item, not reset to the first item.
4. **Obsidian Focus Styling:** Focus indicators must adhere strictly to `docs/foundations/design_system.md`:
   * High-contrast clean edges (Subtle 2dp border with `JellyfinColors.AccentCyan` or `WhitePure`).
   * No neon glows, no glassmorphism, no rainbow aura.
   * No bouncy or cartoon springs; use clean, subtle scale transitions (e.g., 1.03x to 1.05x maximum).

---

## 2. Focus Memory & Restoration Patterns

### Preserving Row Focus with `focusRestorer`

When navigating vertically across multiple `LazyRow` items in a screen:

```kotlin
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer

// Apply focusRestorer to container or lazy row parent to preserve the last active item
LazyRow(
    modifier = Modifier
        .focusRestorer()
        .focusGroup(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(horizontal = 48.dp)
) {
    items(
        items = mediaList,
        key = { item -> item.id }
    ) { mediaItem ->
        MediaPosterCard(
            item = mediaItem,
            onClick = { onMediaSelected(mediaItem) }
        )
    }
}
```

### Initial Focus Assignment

To set initial focus on screen entrance (e.g., hero spotlight or first row):

```kotlin
val focusRequester = remember { FocusRequester() }

LaunchedEffect(Unit) {
    // Request focus once the layout pass completes
    focusRequester.requestFocus()
}

PlayButton(
    modifier = Modifier.focusRequester(focusRequester)
)
```

---

## 3. Composable Focus State & Visual Indication

A properly focused TV card must manage focus state explicitly and smoothly without allocating new objects during recomposition:

```kotlin
@Composable
fun TvMediaCard(
    item: MediaItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tvCardScale"
    )

    val borderStroke = if (isFocused) {
        BorderStroke(2.dp, JellyfinColors.AccentCyan)
    } else {
        BorderStroke(1.dp, JellyfinPrimitives.Slate700)
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(8.dp),
        color = JellyfinPrimitives.Graphite900,
        border = borderStroke
    ) {
        // Card content: Poster image, title, and badges
    }
}
```

---

## 4. 120Hz Smooth Scrolling Checklist

To guarantee 120Hz performance on OLED TVs and high-refresh panels:

* **Always provide stable keys:** Every `items()` call must specify a unique, stable `key = { it.id }`.
* **Zero allocations in composable scopes:** Avoid creating closures, formatters, or calculating layouts directly inside Composable functions without `remember`.
* **State hoisting:** Defer state reads using lambdas where possible to limit recomposition scope.
* **Pre-measured cards:** Cards in lazy layouts must have fixed or explicitly bounded dimensions (`width`, `height`, or aspect ratio) to prevent layout recalculation thrashing during rapid D-pad holding.
