package org.jellyfin.mobile.domain.model

import androidx.compose.runtime.Immutable

/**
 * Immutable domain model representing a single step in the hierarchical breadcrumb path.
 * Pure Kotlin with zero platform dependencies.
 */
@Immutable
data class BreadcrumbItem(
    val id: String,
    val name: String,
    val level: Int = 0
)
