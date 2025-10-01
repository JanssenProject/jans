package io.jans.chip.model.fido.attestation.result

import androidx.room.Ignore

data class AttestationResultRequest(
    var id: String?,

    var type: String?,

    var response: Response?,
) {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}