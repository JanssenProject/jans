package com.example.fido2.model.fido.assertion.result

import androidx.room.Ignore
import kotlinx.serialization.Serializable

@Serializable
class Response {
    var authenticatorData: String? = null
    // ClientDataJSON clientDataJSON;

    var clientDataJSON: String? = null

    var signature: String? = null

    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}