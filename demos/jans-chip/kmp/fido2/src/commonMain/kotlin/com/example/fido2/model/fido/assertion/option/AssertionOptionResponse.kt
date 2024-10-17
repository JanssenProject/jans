package com.example.fido2.model.fido.assertion.option

import androidx.room.Ignore
import com.example.fido2.model.fido.assertion.option.AllowCredentials
import kotlinx.serialization.Serializable

@Serializable
data class AssertionOptionResponse(
    var challenge: String?,

    var user: String? = "",

    var userVerification: String?,

    var rpId: String?,

    var status: String?,

    var errorMessage: String?,

    var allowCredentials: List<AllowCredentials>?,
) {
    @Ignore
    var isSuccessful: Boolean? = true
}