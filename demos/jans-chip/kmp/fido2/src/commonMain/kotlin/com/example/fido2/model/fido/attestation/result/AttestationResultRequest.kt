package com.example.fido2.model.fido.attestation.result

import androidx.room.Ignore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AttestationResultRequest(
    var id: String?,

    var type: String?,

    var response: AttestationResponse?,
) {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null

    fun toJson() = Json.encodeToString(serializer(),this)
}