package com.example.fido2.model.fido.config

import androidx.room.Ignore
import com.example.fido2.model.fido.config.Assertion
import com.example.fido2.model.fido.config.Attestation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FidoConfigurationResponse(
    @SerialName("issuer")
    var issuer: String? = null,

    @SerialName("attestation")
    var attestation: Attestation? = null,

    @SerialName("assertion")
    var assertion: Assertion? = null,
) {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}