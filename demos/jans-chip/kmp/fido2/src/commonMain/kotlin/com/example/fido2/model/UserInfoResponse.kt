package com.example.fido2.model

import androidx.room.Ignore
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class UserInfoResponse (
    @Contextual
    var response: Any? = null
) {
    @Ignore
    var isSuccessful: Boolean? = false

    @Ignore
    var errorMessage: String? = null
}