package io.jans.chip.model.fido.assertion.option

import androidx.room.Ignore

data class AssertionOptionResponse(
    var challenge: String?,

    var user: String?,

    var userVerification: String?,

    var rpId: String?,

    var status: String?,

    var errorMessage: String?,

    var allowCredentials: List<AllowCredentials>?,
) {
    @Ignore
    var isSuccessful: Boolean? = true
}