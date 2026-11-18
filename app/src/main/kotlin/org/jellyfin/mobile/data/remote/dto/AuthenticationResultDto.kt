package org.jellyfin.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationResultDto(
    @SerialName("User") val user: UserDto? = null,
    @SerialName("SessionInfo") val sessionInfo: SessionInfoDto? = null,
    @SerialName("AccessToken") val accessToken: String? = null,
    @SerialName("ServerId") val serverId: String? = null
)

@Serializable
data class UserDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("ServerId") val serverId: String? = null,
    @SerialName("HasPassword") val hasPassword: Boolean? = null,
    @SerialName("Configuration") val configuration: UserConfigurationDto? = null
)

@Serializable
data class UserConfigurationDto(
    @SerialName("AudioLanguagePreference") val audioLanguagePreference: String? = null,
    @SerialName("SubtitleLanguagePreference") val subtitleLanguagePreference: String? = null,
    @SerialName("SubtitleMode") val subtitleMode: String? = null
)

@Serializable
data class SessionInfoDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("UserId") val userId: String? = null,
    @SerialName("UserName") val userName: String? = null
)
