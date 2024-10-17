package com.example.fido2.model.fido.attestation.result

import androidx.room.Ignore
import kotlinx.serialization.Serializable

@Serializable
class AttestationResultResponse{
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}