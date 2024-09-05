package io.jans.chip.model.appIntegrity

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

data class AppIntegrityResponse(
    @SerializedName("requestDetails")
    var requestDetails: RequestDetails? = null,

    @SerializedName("appIntegrity")
    var appIntegrity: AppIntegrity? = null,

    @SerializedName("deviceIntegrity")
    var deviceIntegrity: DeviceIntegrity? = null,

    @SerializedName("accountDetails")
    var accountDetails: AccountDetails? = null,

    @SerializedName("error")
    var error: String? = null
) {
    @Ignore
    var isSuccessful: Boolean? = false

    @Ignore
    var errorMessage: String? = null
}