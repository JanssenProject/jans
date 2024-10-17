package com.example.fido2.model

import androidx.room.Ignore
import kotlinx.serialization.Serializable

@Serializable
class LogoutResponse() {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}