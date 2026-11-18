package org.jellyfin.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicSystemInfoDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("ServerName") val serverName: String? = null,
    @SerialName("Version") val version: String? = null,
    @SerialName("OperatingSystem") val operatingSystem: String? = null,
    @SerialName("LocalAddress") val localAddress: String? = null,
    @SerialName("ProductName") val productName: String? = null,
    @SerialName("StartupWizardCompleted") val startupWizardCompleted: Boolean? = null
)
