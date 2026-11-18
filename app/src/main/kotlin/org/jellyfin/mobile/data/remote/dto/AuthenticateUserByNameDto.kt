package org.jellyfin.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticateUserByNameDto(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val password: String = ""
)
