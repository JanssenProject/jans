package com.example.fido2.model.appIntegrity

import androidx.room.Ignore
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppIntegrityResponse(
    @Contextual
    @SerialName("requestDetails")
    var requestDetails: RequestDetails? = null,

    @Contextual
    @SerialName("appIntegrity")
    var appIntegrity: AppIntegrity? = null,

    @Contextual
    @SerialName("deviceIntegrity")
    var deviceIntegrity: DeviceIntegrity? = null,

    @Contextual
    @SerialName("accountDetails")
    var accountDetails: AccountDetails? = null,

    @SerialName("error")
    var error: String? = null
) {
    @Ignore
    var isSuccessful: Boolean? = false

    @Ignore
    var errorMessage: String? = null
}