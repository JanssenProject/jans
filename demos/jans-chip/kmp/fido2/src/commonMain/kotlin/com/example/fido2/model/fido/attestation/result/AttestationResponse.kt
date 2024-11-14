package com.example.fido2.model.fido.attestation.result

import kotlinx.serialization.Serializable

@Serializable
data class AttestationResponse(
    var attestationObject: String,

    val clientDataJSON: String,
)