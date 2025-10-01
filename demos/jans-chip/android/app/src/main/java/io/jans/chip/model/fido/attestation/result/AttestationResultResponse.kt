package io.jans.chip.model.fido.attestation.result

import androidx.room.Ignore

class AttestationResultResponse{
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}