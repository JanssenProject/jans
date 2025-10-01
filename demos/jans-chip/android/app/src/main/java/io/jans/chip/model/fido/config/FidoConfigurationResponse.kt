package io.jans.chip.model.fido.config

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

data class FidoConfigurationResponse(
    @SerializedName("issuer")
    var issuer: String? = null,

    @SerializedName("attestation")
    var attestation: Attestation? = null,

    @SerializedName("assertion")
    var assertion: Assertion? = null,
) {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}