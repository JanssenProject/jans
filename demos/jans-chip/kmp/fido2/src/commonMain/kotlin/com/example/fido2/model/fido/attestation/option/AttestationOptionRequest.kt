package com.example.fido2.model.fido.attestation.option

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class AttestationOptionRequest(
    var username: String,

    var displayName: String,

    var attestation: String,
) {
    fun toJson() = Json.encodeToString(serializer(),this)
}