package io.jans.chip.model.fido.assertion.result

import androidx.room.Ignore

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