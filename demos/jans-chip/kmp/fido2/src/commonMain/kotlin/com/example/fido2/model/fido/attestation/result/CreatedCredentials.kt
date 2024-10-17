package com.example.fido2.model.fido.attestation.result

import kotlinx.serialization.Serializable

@Serializable
data class CreatedCredentials(
    var createdDate: String,

    val updatedDate: String,

    val createdBy: String,

    val updatedBy: String,

    val username: String,

    val domain: String,

    val userId: String,

    val challenge: String,

    val attestationRequest: String,
)