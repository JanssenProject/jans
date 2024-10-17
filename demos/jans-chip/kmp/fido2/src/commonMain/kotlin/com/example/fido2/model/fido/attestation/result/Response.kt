package com.example.fido2.model.fido.attestation.result

import kotlinx.serialization.Serializable

@Serializable
data class Response(
    var attestationObject: String,

    val clientDataJSON: String,
)