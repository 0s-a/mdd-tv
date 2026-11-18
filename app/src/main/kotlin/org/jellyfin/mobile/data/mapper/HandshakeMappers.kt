package org.jellyfin.mobile.data.mapper

import org.jellyfin.mobile.data.remote.dto.AuthenticationResultDto
import org.jellyfin.mobile.data.remote.dto.BaseItemDto
import org.jellyfin.mobile.data.remote.dto.PublicSystemInfoDto
import org.jellyfin.mobile.data.remote.dto.UserViewsResponseDto
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession

/**
 * Defensive mappers transforming nullable remote DTOs into immutable, safe domain models.
 */

fun PublicSystemInfoDto.toDomain(): ServerInfo {
    return ServerInfo(
        id = id.orEmpty(),
        name = serverName?.takeIf { it.isNotBlank() }
            ?: productName?.takeIf { it.isNotBlank() }
            ?: "Jellyfin Server",
        version = version?.takeIf { it.isNotBlank() } ?: "Unknown",
        operatingSystem = operatingSystem?.takeIf { it.isNotBlank() } ?: "Unknown"
    )
}

fun AuthenticationResultDto.toDomain(): UserSession {
    val u = user
    val conf = u?.configuration
    val sInfo = sessionInfo

    return UserSession(
        userId = u?.id ?: sInfo?.userId.orEmpty(),
        userName = u?.name ?: sInfo?.userName.orEmpty(),
        token = accessToken.orEmpty(),
        subtitleLanguagePreference = conf?.subtitleLanguagePreference?.takeIf { it.isNotBlank() } ?: "ara",
        subtitleMode = conf?.subtitleMode?.takeIf { it.isNotBlank() } ?: "Always"
    )
}

fun BaseItemDto.toDomain(): LibraryFolder {
    return LibraryFolder(
        id = id.orEmpty(),
        name = name?.takeIf { it.isNotBlank() } ?: "Unnamed Folder",
        type = collectionType?.takeIf { it.isNotBlank() }
            ?: type?.takeIf { it.isNotBlank() }
            ?: "CollectionFolder"
    )
}

fun UserViewsResponseDto.toDomain(): List<LibraryFolder> {
    return items?.map { it.toDomain() } ?: emptyList()
}
