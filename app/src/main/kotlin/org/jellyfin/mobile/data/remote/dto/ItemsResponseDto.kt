package org.jellyfin.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemsResponseDto(
    @SerialName("Items") val items: List<BaseItemDto>? = null,
    @SerialName("TotalRecordCount") val totalRecordCount: Int? = null,
    @SerialName("StartIndex") val startIndex: Int? = null
)
